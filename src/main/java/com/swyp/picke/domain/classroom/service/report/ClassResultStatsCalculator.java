package com.swyp.picke.domain.classroom.service.report;

import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.battle.repository.BattleOptionRepository;
import com.swyp.picke.domain.classroom.entity.ClassMember;
import com.swyp.picke.domain.classroom.entity.Classroom;
import com.swyp.picke.domain.classroom.enums.ClassBattleType;
import com.swyp.picke.domain.classroom.repository.ClassMemberRepository;
import com.swyp.picke.domain.perspective.entity.Perspective;
import com.swyp.picke.domain.perspective.enums.PerspectiveStatus;
import com.swyp.picke.domain.perspective.repository.PerspectiveRepository;
import com.swyp.picke.domain.user.entity.UserBattle;
import com.swyp.picke.domain.user.enums.UserBattleStep;
import com.swyp.picke.domain.user.repository.UserBattleRepository;
import com.swyp.picke.domain.vote.entity.BattleVote;
import com.swyp.picke.domain.vote.repository.BattleVoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 클래스 현재 데이터로 정량 통계와 참여 완료자를 계산한다.
 * 마감 전 결과 화면(실시간)과 마감 스냅샷 생성이 같은 계산을 쓴다 (PRD §15 "마감 전 집계는 현재 활성 참여자").
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClassResultStatsCalculator {

    private final ClassMemberRepository classMemberRepository;
    private final BattleOptionRepository battleOptionRepository;
    private final UserBattleRepository userBattleRepository;
    private final BattleVoteRepository battleVoteRepository;
    private final PerspectiveRepository perspectiveRepository;

    /** 통계 + 참여 완료 멤버 목록 */
    public record Result(ClassResultStats stats, List<ClassMember> completedMembers) {
    }

    public Result calculate(Classroom classroom) {
        Long classId = classroom.getId();
        List<ClassMember> activeMembers = classMemberRepository.findActiveMembers(classId);
        Map<Long, ClassMember> activeByUserId = new HashMap<>();
        for (ClassMember member : activeMembers) {
            activeByUserId.put(member.getUser().getId(), member);
        }

        Set<Long> writerUserIds = new HashSet<>();
        for (Perspective p : perspectiveRepository.findByClassroomIdAndStatusOrderByCreatedAtAsc(classId, PerspectiveStatus.PUBLISHED)) {
            Long userId = p.getUser().getId();
            if (activeByUserId.containsKey(userId)) {
                writerUserIds.add(userId);
            }
        }

        List<ClassMember> completedMembers = new ArrayList<>();
        for (UserBattle ub : userBattleRepository.findByClassroomId(classId)) {
            Long userId = ub.getUser().getId();
            ClassMember member = activeByUserId.get(userId);
            if (member == null || ub.getStep() != UserBattleStep.COMPLETED) {
                continue;
            }
            if (classroom.isCommentRequired() && !writerUserIds.contains(userId)) {
                continue;
            }
            completedMembers.add(member);
        }
        completedMembers.sort(Comparator.comparing(ClassMember::getId));

        List<BattleOption> options = battleOptionRepository.findByBattle(classroom.getBattle());
        options.sort(Comparator.comparing(BattleOption::getDisplayOrder, Comparator.nullsLast(Comparator.naturalOrder())));
        boolean midVoteEnabled = classroom.getBattleType() == ClassBattleType.TTS && classroom.getBattle().isMidVoteEnabled();
        Map<Long, int[]> countsByOption = new HashMap<>(); // [pre, mid, post]
        for (BattleOption option : options) {
            countsByOption.put(option.getId(), new int[3]);
        }
        int postVoteCount = 0;
        int midVoteCount = 0;
        for (BattleVote vote : battleVoteRepository.findByClassroomId(classId)) {
            if (!activeByUserId.containsKey(vote.getUser().getId())) {
                continue;
            }
            if (vote.getPreVoteOption() != null) {
                int[] counts = countsByOption.get(vote.getPreVoteOption().getId());
                if (counts != null) counts[0]++;
            }
            if (midVoteEnabled && vote.getMidVoteOption() != null) {
                int[] counts = countsByOption.get(vote.getMidVoteOption().getId());
                if (counts != null) counts[1]++;
                midVoteCount++;
            }
            if (vote.getPostVoteOption() != null) {
                int[] counts = countsByOption.get(vote.getPostVoteOption().getId());
                if (counts != null) counts[2]++;
                postVoteCount++;
            }
        }

        List<ClassResultStats.OptionStat> optionStats = new ArrayList<>();
        for (BattleOption option : options) {
            int[] counts = countsByOption.get(option.getId());
            optionStats.add(new ClassResultStats.OptionStat(
                    option.getId(), option.getTitle(), option.getStance(),
                    counts[0], counts[1], counts[2], ClassResultStats.rate(counts[2], postVoteCount)));
        }

        int active = activeMembers.size();
        ClassResultStats stats = new ClassResultStats(
                active,
                completedMembers.size(),
                ClassResultStats.rate(completedMembers.size(), active),
                writerUserIds.size(),
                ClassResultStats.rate(writerUserIds.size(), active),
                postVoteCount,
                midVoteEnabled,
                midVoteCount,
                optionStats
        );
        return new Result(stats, completedMembers);
    }
}

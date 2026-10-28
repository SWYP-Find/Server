package com.swyp.picke.domain.classroom.service.report;

import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.battle.repository.BattleOptionRepository;
import com.swyp.picke.domain.classroom.entity.ClassMember;
import com.swyp.picke.domain.classroom.entity.ClassMidVote;
import com.swyp.picke.domain.classroom.entity.Classroom;
import com.swyp.picke.domain.classroom.repository.ClassMemberRepository;
import com.swyp.picke.domain.classroom.repository.ClassMidVoteRepository;
import com.swyp.picke.domain.classroom.service.ClassMidVoteService;
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
import java.util.LinkedHashMap;
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
    private final ClassMidVoteRepository classMidVoteRepository;
    private final ClassMidVoteService classMidVoteService;

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
        Map<Long, int[]> countsByOption = new HashMap<>(); // [pre, post]
        for (BattleOption option : options) {
            countsByOption.put(option.getId(), new int[2]);
        }
        int postVoteCount = 0;
        for (BattleVote vote : battleVoteRepository.findByClassroomId(classId)) {
            if (!activeByUserId.containsKey(vote.getUser().getId())) {
                continue;
            }
            if (vote.getPreVoteOption() != null) {
                int[] counts = countsByOption.get(vote.getPreVoteOption().getId());
                if (counts != null) counts[0]++;
            }
            if (vote.getPostVoteOption() != null) {
                int[] counts = countsByOption.get(vote.getPostVoteOption().getId());
                if (counts != null) counts[1]++;
                postVoteCount++;
            }
        }

        List<ClassResultStats.OptionStat> optionStats = new ArrayList<>();
        for (BattleOption option : options) {
            int[] counts = countsByOption.get(option.getId());
            optionStats.add(new ClassResultStats.OptionStat(
                    option.getId(), option.getTitle(), option.getStance(),
                    counts[0], counts[1], ClassResultStats.rate(counts[1], postVoteCount)));
        }

        boolean midVoteEnabled = classMidVoteService.enabled(classroom);
        Set<Long> midVoterUserIds = new HashSet<>();
        List<ClassResultStats.CheckpointStat> checkpointStats = midVoteEnabled
                ? midVoteCheckpoints(classroom, activeByUserId.keySet(), midVoterUserIds)
                : List.of();

        int active = activeMembers.size();
        ClassResultStats stats = new ClassResultStats(
                active,
                completedMembers.size(),
                ClassResultStats.rate(completedMembers.size(), active),
                writerUserIds.size(),
                ClassResultStats.rate(writerUserIds.size(), active),
                postVoteCount,
                midVoteEnabled,
                midVoterUserIds.size(),
                optionStats,
                checkpointStats
        );
        return new Result(stats, completedMembers);
    }

    /**
     * 선택 지점별 중간투표 집계. 현재 시나리오의 지점·선택지를 0명부터 채우고,
     * 시나리오 수정으로 사라진 선택지에 남은 기록은 저장된 문구로 뒤에 붙인다.
     */
    private List<ClassResultStats.CheckpointStat> midVoteCheckpoints(Classroom classroom, Set<Long> activeUserIds, Set<Long> voterUserIds) {
        Map<Long, Map<Long, String>> labels = new LinkedHashMap<>(); // checkpoint -> option -> label
        for (ClassMidVoteService.Checkpoint checkpoint : classMidVoteService.checkpoints(classroom.getBattle())) {
            Map<Long, String> choices = new LinkedHashMap<>();
            for (ClassMidVoteService.Choice choice : checkpoint.options()) {
                choices.put(choice.optionId(), choice.label());
            }
            labels.put(checkpoint.checkpointId(), choices);
        }
        Map<Long, Map<Long, Integer>> counts = new HashMap<>();
        for (ClassMidVote midVote : classMidVoteRepository.findByClassId(classroom.getId())) {
            Long userId = midVote.getVote().getUser().getId();
            if (!activeUserIds.contains(userId)) {
                continue;
            }
            voterUserIds.add(userId);
            labels.computeIfAbsent(midVote.getCheckpointId(), id -> new LinkedHashMap<>())
                    .putIfAbsent(midVote.getOptionId(), midVote.getOptionLabel());
            counts.computeIfAbsent(midVote.getCheckpointId(), id -> new HashMap<>())
                    .merge(midVote.getOptionId(), 1, Integer::sum);
        }

        List<ClassResultStats.CheckpointStat> stats = new ArrayList<>();
        for (Map.Entry<Long, Map<Long, String>> checkpoint : labels.entrySet()) {
            Map<Long, Integer> byOption = counts.getOrDefault(checkpoint.getKey(), Map.of());
            int total = byOption.values().stream().mapToInt(Integer::intValue).sum();
            List<ClassResultStats.ChoiceStat> choices = new ArrayList<>();
            for (Map.Entry<Long, String> option : checkpoint.getValue().entrySet()) {
                int count = byOption.getOrDefault(option.getKey(), 0);
                choices.add(new ClassResultStats.ChoiceStat(option.getKey(), option.getValue(), count, ClassResultStats.rate(count, total)));
            }
            stats.add(new ClassResultStats.CheckpointStat(checkpoint.getKey(), total, choices));
        }
        return stats;
    }
}

package com.swyp.picke.domain.classroom.service;

import com.swyp.picke.domain.battle.dto.response.BattleUserDetailResponse;
import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.service.BattleService;
import com.swyp.picke.domain.battle.util.BattleOptionDisplay;
import com.swyp.picke.domain.classroom.dto.response.ClassBattleDetailResponse;
import com.swyp.picke.domain.classroom.entity.Classroom;
import com.swyp.picke.domain.classroom.service.ClassAccessService.ClassAccess;
import com.swyp.picke.domain.perspective.dto.request.CreatePerspectiveRequest;
import com.swyp.picke.domain.perspective.dto.response.CreatePerspectiveResponse;
import com.swyp.picke.domain.perspective.dto.response.MyPerspectiveResponse;
import com.swyp.picke.domain.perspective.dto.response.PerspectiveListResponse;
import com.swyp.picke.domain.perspective.enums.PerspectiveStatus;
import com.swyp.picke.domain.perspective.repository.PerspectiveRepository;
import com.swyp.picke.domain.perspective.service.PerspectiveService;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.enums.UserBattleStep;
import com.swyp.picke.domain.user.enums.VoteSide;
import com.swyp.picke.domain.user.repository.UserRepository;
import com.swyp.picke.domain.user.service.UserBattleService;
import com.swyp.picke.domain.vote.dto.request.VoteRequest;
import com.swyp.picke.domain.vote.dto.response.MyVoteResponse;
import com.swyp.picke.domain.vote.dto.response.VoteResultResponse;
import com.swyp.picke.domain.vote.dto.response.VoteStatsResponse;
import com.swyp.picke.domain.vote.repository.BattleVoteRepository;
import com.swyp.picke.domain.vote.service.BattleVoteService;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 클래스 배틀 진행. 접근 판정은 {@link ClassAccessService}, 실제 투표·의견 처리는 기존 서비스의 classroom 오버로드에 위임한다.
 * <ul>
 *   <li>일반 배틀과의 격리: 같은 사용자가 같은 배틀을 일반에서도 진행할 수 있고 둘은 서로 영향을 주지 않는다 (PRD §3)</li>
 *   <li>크레딧 차감·성향 점수·참여자 수 집계·SSE 는 클래스 투표에 적용하지 않는다</li>
 *   <li>참여 완료 = 사후투표 완료 + (의견 필수면) 내 PUBLISHED 의견 1건 이상 (PRD F10)</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClassBattleServiceImpl implements ClassBattleService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final ClassAccessService classAccessService;
    private final BattleService battleService;
    private final BattleVoteService battleVoteService;
    private final UserBattleService userBattleService;
    private final PerspectiveService perspectiveService;
    private final BattleVoteRepository battleVoteRepository;
    private final PerspectiveRepository perspectiveRepository;
    private final UserRepository userRepository;

    @Override
    public ClassBattleDetailResponse getDetail(Long userId, Long classId) {
        ClassAccess access = classAccessService.requireReadable(classId, userId);
        Classroom classroom = access.classroom();
        Battle battle = battleOf(classroom);
        User user = findUser(userId);
        LocalDateTime now = LocalDateTime.now(KST);

        UserBattleStep step = userBattleService.getUserBattleStatus(user, battle, classroom).step();
        VoteSide voteSide = battleVoteRepository.findByBattleAndUserAndClassroom(battle, user, classroom)
                .map(vote -> vote.getPostVoteOption() != null ? BattleOptionDisplay.voteSide(vote.getPostVoteOption()) : null)
                .orElse(null);
        long participantsCount = battleVoteRepository.countByBattleAndClassroom(battle, classroom);

        boolean closed = classroom.isClosed(now);
        boolean canParticipate = access.canWrite() && !closed;
        boolean completed = step == UserBattleStep.COMPLETED
                && (!classroom.isCommentRequired() || hasPublishedPerspective(battle, userId, classroom));

        BattleUserDetailResponse detail = battleService.buildUserDetail(battle, participantsCount, voteSide, step);
        return new ClassBattleDetailResponse(
                classroom.getId(),
                access.role(),
                classroom.getEffectiveCloseAt(),
                closed,
                classroom.isCommentRequired(),
                canParticipate,
                completed,
                detail
        );
    }

    @Override
    @Transactional
    public VoteResultResponse preVote(Long userId, Long classId, VoteRequest request) {
        Classroom classroom = writable(classId, userId);
        return battleVoteService.preVote(battleOf(classroom).getId(), userId, classroom, request);
    }

    @Override
    @Transactional
    public void completeTts(Long userId, Long classId) {
        Classroom classroom = writable(classId, userId);
        battleVoteService.completeTts(battleOf(classroom).getId(), userId, classroom);
    }

    @Override
    @Transactional
    public VoteResultResponse postVote(Long userId, Long classId, VoteRequest request) {
        Classroom classroom = writable(classId, userId);
        return battleVoteService.postVote(battleOf(classroom).getId(), userId, classroom, request);
    }

    @Override
    public MyVoteResponse getMyVote(Long userId, Long classId) {
        Classroom classroom = readable(classId, userId);
        return battleVoteService.getMyVote(battleOf(classroom).getId(), userId, classroom);
    }

    @Override
    public VoteStatsResponse getVoteStats(Long userId, Long classId) {
        Classroom classroom = readable(classId, userId);
        return battleVoteService.getVoteStats(battleOf(classroom).getId(), classroom);
    }

    @Override
    @Transactional
    public CreatePerspectiveResponse createPerspective(Long userId, Long classId, CreatePerspectiveRequest request) {
        Classroom classroom = writable(classId, userId);
        return perspectiveService.createPerspective(battleOf(classroom).getId(), userId, classroom, request);
    }

    @Override
    public PerspectiveListResponse getPerspectives(Long userId, Long classId, String cursor, Integer size, Long optionId, String sort) {
        Classroom classroom = readable(classId, userId);
        return perspectiveService.getPerspectives(battleOf(classroom).getId(), userId, classroom, cursor, size, optionId, sort);
    }

    @Override
    public List<MyPerspectiveResponse> getMyPerspectives(Long userId, Long classId) {
        Classroom classroom = readable(classId, userId);
        return perspectiveService.getMyPerspective(battleOf(classroom).getId(), userId, classroom);
    }

    // ===== 헬퍼 =====

    private Classroom readable(Long classId, Long userId) {
        return classAccessService.requireReadable(classId, userId).classroom();
    }

    private Classroom writable(Long classId, Long userId) {
        return classAccessService.requireWritable(classId, userId, LocalDateTime.now(KST)).classroom();
    }

    private boolean hasPublishedPerspective(Battle battle, Long userId, Classroom classroom) {
        return perspectiveRepository.existsByBattleIdAndUserIdAndClassroomIdAndStatus(
                battle.getId(), userId, classroom.getId(), PerspectiveStatus.PUBLISHED);
    }

    private static Battle battleOf(Classroom classroom) {
        Battle battle = classroom.getBattle();
        if (battle == null) {
            throw new CustomException(ErrorCode.CLASS_BATTLE_NOT_AVAILABLE);
        }
        return battle;
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}

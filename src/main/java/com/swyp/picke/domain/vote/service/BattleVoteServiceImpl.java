package com.swyp.picke.domain.vote.service;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.battle.repository.BattleOptionRepository;
import com.swyp.picke.domain.battle.repository.BattleOptionTagRepository;
import com.swyp.picke.domain.battle.service.BattleService;
import com.swyp.picke.domain.classroom.entity.Classroom;
import com.swyp.picke.domain.tag.enums.TagType;
import com.swyp.picke.domain.user.dto.response.UserBattleStatusResponse;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.entity.UserTendencyScore;
import com.swyp.picke.domain.user.enums.CreditType;
import com.swyp.picke.domain.user.enums.UserBattleStep;
import com.swyp.picke.domain.user.repository.UserRepository;
import com.swyp.picke.domain.user.repository.UserTendencyScoreRepository;
import com.swyp.picke.domain.user.service.CreditService;
import com.swyp.picke.domain.user.service.UserBattleService;
import com.swyp.picke.domain.vote.converter.VoteConverter;
import com.swyp.picke.domain.vote.dto.request.VoteRequest;
import com.swyp.picke.domain.vote.dto.response.MyVoteResponse;
import com.swyp.picke.domain.vote.dto.response.VoteResultResponse;
import com.swyp.picke.domain.vote.dto.response.VoteStatsResponse;
import com.swyp.picke.domain.vote.entity.BattleVote;
import com.swyp.picke.domain.vote.repository.BattleVoteRepository;
import com.swyp.picke.domain.vote.sse.VoteUpdatedEvent;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import com.swyp.picke.global.infra.s3.enums.FileCategory;
import com.swyp.picke.global.infra.s3.util.ResourceUrlProvider;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 일반 배틀과 클래스 내 배틀은 같은 테이블(votes/user_battles)에 class_id 로 구분해 저장한다.
 * 클래스 스코프(classroom != null)에서는 다음이 적용되지 않는다 (PRD §7, ERD votes 노트):
 * 크레딧 차감(BATTLE_ENTRY)·오늘의 배틀 무료 진입 판정, battles.total_participants 증가, 성향 점수(value tag) 반영, SSE 통계 이벤트.
 * 대신 사전투표는 콘텐츠 시작 후 잠기고, 사후투표는 제출 후 잠긴다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BattleVoteServiceImpl implements BattleVoteService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final BattleVoteRepository battleVoteRepository;
    private final BattleService battleService;
    private final BattleOptionRepository battleOptionRepository;
    private final BattleOptionTagRepository battleOptionTagRepository;
    private final UserRepository userRepository;
    private final UserTendencyScoreRepository userTendencyScoreRepository;
    private final UserBattleService userBattleService;
    private final CreditService creditService;
    private final ApplicationEventPublisher eventPublisher;
    private final ResourceUrlProvider urlProvider;

    @Override
    public BattleOption findPreVoteOption(Long battleId, Long userId) {
        Battle battle = battleService.findById(battleId);
        User user = findUser(userId);

        BattleVote vote = battleVoteRepository.findByBattleAndUserAndClassroom(battle, user, null)
                .orElseThrow(() -> new CustomException(ErrorCode.VOTE_NOT_FOUND));

        if (vote.getPreVoteOption() == null) {
            throw new CustomException(ErrorCode.PRE_VOTE_REQUIRED);
        }
        return vote.getPreVoteOption();
    }

    @Override
    public Long findPostVoteOptionId(Long battleId, Long userId) {
        return battleVoteRepository.findByBattleIdAndUserIdAndClassroomIsNull(battleId, userId)
                .map(vote -> vote.getPostVoteOption() != null ? vote.getPostVoteOption().getId() : null)
                .orElse(null);
    }

    @Override
    public Long findPostVoteOptionId(Long battleId, Long userId, Classroom classroom) {
        if (classroom == null) {
            return findPostVoteOptionId(battleId, userId);
        }
        Battle battle = battleService.findById(battleId);
        User user = findUser(userId);
        return battleVoteRepository.findByBattleAndUserAndClassroom(battle, user, classroom)
                .map(vote -> vote.getPostVoteOption() != null ? vote.getPostVoteOption().getId() : null)
                .orElse(null);
    }

    @Override
    public VoteStatsResponse getVoteStats(Long battleId) {
        return getVoteStats(battleId, null);
    }

    @Override
    public VoteStatsResponse getVoteStats(Long battleId, Classroom classroom) {
        Battle battle = battleService.findById(battleId);
        List<BattleOption> options = battleOptionRepository.findByBattle(battle);
        long totalCount = battleVoteRepository.countByBattleAndClassroomAndPostVoteOptionIsNotNull(battle, classroom);

        List<VoteStatsResponse.OptionStat> stats = options.stream()
                .map(option -> {
                    long count = battleVoteRepository.countByBattleAndClassroomAndPostVoteOption(battle, classroom, option);
                    double ratio = totalCount > 0
                            ? Math.round((double) count / totalCount * 1000.0) / 10.0
                            : 0.0;
                    return new VoteStatsResponse.OptionStat(
                            option.getId(),
                            option.getTitle(),
                            urlProvider.getImageUrl(FileCategory.PHILOSOPHER, option.getImageUrl()),
                            count,
                            ratio
                    );
                })
                .toList();

        LocalDateTime updatedAt = battleVoteRepository.findTopByBattleAndClassroomOrderByUpdatedAtDesc(battle, classroom)
                .map(BattleVote::getUpdatedAt)
                .orElse(null);

        return VoteConverter.toVoteStatsResponse(stats, totalCount, updatedAt);
    }

    @Override
    public MyVoteResponse getMyVote(Long battleId, Long userId) {
        return getMyVote(battleId, userId, null);
    }

    @Override
    public MyVoteResponse getMyVote(Long battleId, Long userId, Classroom classroom) {
        Battle battle = battleService.findById(battleId);
        User user = findUser(userId);

        BattleVote vote = battleVoteRepository.findByBattleAndUserAndClassroom(battle, user, classroom)
                .orElseThrow(() -> new CustomException(ErrorCode.VOTE_NOT_FOUND));

        UserBattleStatusResponse status = userBattleService.getUserBattleStatus(user, battle, classroom);
        return VoteConverter.toMyVoteResponse(vote, status.step());
    }

    @Override
    @Transactional
    public VoteResultResponse preVote(Long battleId, Long userId, VoteRequest request) {
        return preVote(battleId, userId, null, request);
    }

    @Override
    @Transactional
    public VoteResultResponse preVote(Long battleId, Long userId, Classroom classroom, VoteRequest request) {
        Battle battle = battleService.findById(battleId);
        User user = findUser(userId);
        BattleOption option = findOptionOf(battle, request.optionId());

        UserBattleStatusResponse status = userBattleService.getUserBattleStatus(user, battle, classroom);
        Optional<BattleVote> existingVote = battleVoteRepository.findByBattleAndUserAndClassroom(battle, user, classroom);
        BattleVote vote;

        if (existingVote.isPresent()) {
            // 클래스: 콘텐츠(배틀)가 시작된 뒤에는 사전투표를 바꿀 수 없다 (PRD §7)
            if (classroom != null && isPreVoteLocked(status.step())) {
                throw new CustomException(ErrorCode.CLASS_PRE_VOTE_LOCKED);
            }
            vote = existingVote.get();
            vote.updatePreVote(option);
        } else {
            if (classroom == null && shouldChargeBattleEntryCredit(battle, user)) {
                creditService.addCredit(user.getId(), CreditType.BATTLE_ENTRY, battle.getId());
            }
            vote = BattleVote.createPreVote(user, battle, classroom, option);
            battleVoteRepository.save(vote);
            if (classroom == null) {
                battle.addParticipant();
            }
        }

        if (status.step() == UserBattleStep.NONE) {
            userBattleService.upsertStep(user, battle, classroom, UserBattleStep.PRE_VOTE);
        }

        UserBattleStep currentStep = status.step() == UserBattleStep.NONE
                ? UserBattleStep.PRE_VOTE
                : status.step();
        return new VoteResultResponse(vote.getId(), currentStep);
    }

    @Override
    @Transactional
    public VoteResultResponse postVote(Long battleId, Long userId, VoteRequest request) {
        return postVote(battleId, userId, null, request);
    }

    @Override
    @Transactional
    public VoteResultResponse postVote(Long battleId, Long userId, Classroom classroom, VoteRequest request) {
        Battle battle = battleService.findById(battleId);
        User user = findUser(userId);
        BattleOption option = findOptionOf(battle, request.optionId());

        BattleVote vote = battleVoteRepository.findByBattleAndUserAndClassroom(battle, user, classroom)
                .orElseThrow(() -> new CustomException(ErrorCode.VOTE_NOT_FOUND));

        UserBattleStatusResponse status = userBattleService.getUserBattleStatus(user, battle, classroom);
        if (status.step() == UserBattleStep.NONE) {
            throw new CustomException(ErrorCode.PRE_VOTE_REQUIRED);
        }
        // 클래스: 사후투표는 제출 후 잠긴다 (PRD §7). 일반 배틀은 기존대로 변경 허용(성향 점수 재계산)
        if (classroom != null && status.step() == UserBattleStep.COMPLETED) {
            throw new CustomException(ErrorCode.VOTE_ALREADY_SUBMITTED);
        }

        BattleOption previousOption = vote.getPostVoteOption();
        vote.doPostVote(option);
        if (classroom == null) {
            userTendencyScoreRepository.findByUserId(userId).ifPresent(score -> {
                if (previousOption != null) {
                    applyValueTags(score, previousOption, -1);
                }
                applyValueTags(score, option, 1);
            });
        }
        userBattleService.upsertStep(user, battle, classroom, UserBattleStep.COMPLETED);
        if (classroom == null) {
            eventPublisher.publishEvent(new VoteUpdatedEvent(battleId));
        }

        return new VoteResultResponse(vote.getId(), UserBattleStep.COMPLETED);
    }

    private void applyValueTags(UserTendencyScore score, BattleOption option, int multiplier) {
        battleOptionTagRepository.findByBattleOption(option).stream()
                .map(optionTag -> optionTag.getTag())
                .filter(tag -> tag.getType() == TagType.VALUE)
                .forEach(tag -> score.applyValueTag(tag.getName(), multiplier));
    }

    /** 어드민 배틀 투표 초기화: 일반 배틀 투표만 지운다. 클래스 내 투표는 클래스 결과의 일부라 건드리지 않는다 */
    @Override
    @Transactional
    public void deleteVotesByBattleId(Long battleId) {
        Battle battle = battleService.findById(battleId);
        List<BattleVote> votes = battleVoteRepository.findAllByBattleAndClassroomIsNull(battle);

        for (BattleVote vote : votes) {
            userBattleService.upsertStep(vote.getUser(), battle, UserBattleStep.NONE);
        }

        battleVoteRepository.deleteAllInBatch(votes);
    }

    @Override
    @Transactional
    public void completeTts(Long battleId, Long userId) {
        completeTts(battleId, userId, null);
    }

    @Override
    @Transactional
    public void completeTts(Long battleId, Long userId, Classroom classroom) {
        Battle battle = battleService.findById(battleId);
        User user = findUser(userId);

        BattleVote vote = battleVoteRepository.findByBattleAndUserAndClassroom(battle, user, classroom)
                .orElseThrow(() -> new CustomException(ErrorCode.VOTE_NOT_FOUND));
        vote.completeTts();

        // 클래스: 이미 사후투표까지 끝났으면 다시 듣기(replay)가 단계를 되돌리지 않는다 (PRD §7)
        if (classroom != null) {
            UserBattleStatusResponse status = userBattleService.getUserBattleStatus(user, battle, classroom);
            if (status.step() == UserBattleStep.COMPLETED) {
                return;
            }
        }
        userBattleService.upsertStep(user, battle, classroom, UserBattleStep.POST_VOTE);
    }

    /**
     * 클래스 중간투표 (PRD §7 "기존 콘텐츠에 해당 단계가 있으면 수집"). 일반 배틀에는 쓰지 않는다.
     * <ul>
     *   <li>mid_vote_enabled 가 아닌 배틀이면 409 CLASS_409_MID_VOTE</li>
     *   <li>사전투표 행이 있어야 하고(없으면 404), 사후투표 전까지는 다시 고를 수 있다. 사후투표 후에는 409 CLASS_409_MID_VOTE_LOCKED</li>
     *   <li>중간투표는 콘텐츠 재생 중에 나오므로 진행 단계를 TTS_LISTENING 으로 올려 사전투표를 잠근다</li>
     * </ul>
     */
    @Override
    @Transactional
    public VoteResultResponse midVote(Long battleId, Long userId, Classroom classroom, VoteRequest request) {
        Battle battle = battleService.findById(battleId);
        if (!battle.isMidVoteEnabled()) {
            throw new CustomException(ErrorCode.CLASS_MID_VOTE_NOT_SUPPORTED);
        }
        User user = findUser(userId);
        BattleOption option = findOptionOf(battle, request.optionId());

        BattleVote vote = battleVoteRepository.findByBattleAndUserAndClassroom(battle, user, classroom)
                .orElseThrow(() -> new CustomException(ErrorCode.VOTE_NOT_FOUND));
        UserBattleStep step = userBattleService.getUserBattleStatus(user, battle, classroom).step();
        if (step == UserBattleStep.COMPLETED) {
            throw new CustomException(ErrorCode.CLASS_MID_VOTE_LOCKED);
        }
        vote.doMidVote(option);
        if (step == UserBattleStep.NONE || step == UserBattleStep.PRE_VOTE) {
            userBattleService.upsertStep(user, battle, classroom, UserBattleStep.TTS_LISTENING);
            step = UserBattleStep.TTS_LISTENING;
        }
        return new VoteResultResponse(vote.getId(), step);
    }

    // 사전투표 잠금: 콘텐츠 시작(사전투표 단계 통과) 이후
    private static boolean isPreVoteLocked(UserBattleStep step) {
        return step != UserBattleStep.NONE && step != UserBattleStep.PRE_VOTE;
    }

    // 오늘의 배틀(targetDate == 오늘)은 하루 1회에 한해 무료 진입, 그 외 진입 또는 이미 오늘 무료 진입을 사용한 경우 -5P 차감
    private boolean shouldChargeBattleEntryCredit(Battle battle, User user) {
        LocalDate today = LocalDate.now(KST);
        boolean isTodayBattle = battle.getTargetDate() != null && battle.getTargetDate().isEqual(today);
        if (!isTodayBattle) {
            return true;
        }
        return battleVoteRepository.existsByUserIdAndClassroomIsNullAndBattle_TargetDate(user.getId(), today);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    // 다른 배틀의 옵션 id 로 투표하는 것을 막는다
    private BattleOption findOptionOf(Battle battle, Long optionId) {
        if (optionId == null) {
            throw new CustomException(ErrorCode.BATTLE_OPTION_NOT_FOUND);
        }
        BattleOption option = battleOptionRepository.findById(optionId)
                .orElseThrow(() -> new CustomException(ErrorCode.BATTLE_OPTION_NOT_FOUND));
        if (option.getBattle() == null || !battle.getId().equals(option.getBattle().getId())) {
            throw new CustomException(ErrorCode.BATTLE_OPTION_NOT_FOUND);
        }
        return option;
    }
}

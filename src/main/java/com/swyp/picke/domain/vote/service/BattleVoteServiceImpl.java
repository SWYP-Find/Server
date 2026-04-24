package com.swyp.picke.domain.vote.service;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.battle.repository.BattleOptionRepository;
import com.swyp.picke.domain.battle.service.BattleService;
import com.swyp.picke.domain.user.dto.response.UserBattleStatusResponse;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.enums.CreditType;
import com.swyp.picke.domain.user.enums.UserBattleStep;
import com.swyp.picke.domain.user.repository.UserRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BattleVoteServiceImpl implements BattleVoteService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final BattleVoteRepository battleVoteRepository;
    private final BattleService battleService;
    private final BattleOptionRepository battleOptionRepository;
    private final UserRepository userRepository;
    private final UserBattleService userBattleService;
    private final CreditService creditService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public BattleOption findPreVoteOption(Long battleId, Long userId) {
        Battle battle = battleService.findById(battleId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        BattleVote vote = battleVoteRepository.findByBattleAndUser(battle, user)
                .orElseThrow(() -> new CustomException(ErrorCode.VOTE_NOT_FOUND));

        if (vote.getPreVoteOption() == null) {
            throw new CustomException(ErrorCode.PRE_VOTE_REQUIRED);
        }
        return vote.getPreVoteOption();
    }

    @Override
    public Long findPostVoteOptionId(Long battleId, Long userId) {
        return battleVoteRepository.findByBattleIdAndUserId(battleId, userId)
                .map(vote -> vote.getPostVoteOption() != null ? vote.getPostVoteOption().getId() : null)
                .orElse(null);
    }

    @Override
    public VoteStatsResponse getVoteStats(Long battleId) {
        Battle battle = battleService.findById(battleId);
        List<BattleOption> options = battleOptionRepository.findByBattle(battle);
        long totalCount = battleVoteRepository.countByBattle(battle);

        List<VoteStatsResponse.OptionStat> stats = options.stream()
                .map(option -> {
                    long count = battleVoteRepository.countByBattleAndPreVoteOption(battle, option);
                    double ratio = totalCount > 0
                            ? Math.round((double) count / totalCount * 1000.0) / 10.0
                            : 0.0;
                    return new VoteStatsResponse.OptionStat(
                            option.getId(),
                            option.getLabel().name(),
                            option.getTitle(),
                            count,
                            ratio
                    );
                })
                .toList();

        LocalDateTime updatedAt = battleVoteRepository.findTopByBattleOrderByUpdatedAtDesc(battle)
                .map(BattleVote::getUpdatedAt)
                .orElse(null);

        return VoteConverter.toVoteStatsResponse(stats, totalCount, updatedAt);
    }

    @Override
    public MyVoteResponse getMyVote(Long battleId, Long userId) {
        Battle battle = battleService.findById(battleId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        BattleVote vote = battleVoteRepository.findByBattleAndUser(battle, user)
                .orElseThrow(() -> new CustomException(ErrorCode.VOTE_NOT_FOUND));

        UserBattleStatusResponse status = userBattleService.getUserBattleStatus(user, battle);
        return VoteConverter.toMyVoteResponse(vote, status.step());
    }

    @Override
    @Transactional
    public VoteResultResponse preVote(Long battleId, Long userId, VoteRequest request) {
        Battle battle = battleService.findById(battleId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        BattleOption option = battleOptionRepository.findById(request.optionId())
                .orElseThrow(() -> new CustomException(ErrorCode.BATTLE_OPTION_NOT_FOUND));

        Optional<BattleVote> existingVote = battleVoteRepository.findByBattleAndUser(battle, user);
        BattleVote vote;

        if (existingVote.isPresent()) {
            vote = existingVote.get();
            vote.updatePreVote(option);
        } else {
            if (shouldChargeBattleEntryCredit(battle)) {
                creditService.addCredit(user.getId(), CreditType.BATTLE_ENTRY, battle.getId());
            }
            vote = BattleVote.createPreVote(user, battle, option);
            battleVoteRepository.save(vote);
            battle.addParticipant();
        }

        UserBattleStatusResponse status = userBattleService.getUserBattleStatus(user, battle);
        if (status.step() == UserBattleStep.NONE) {
            userBattleService.upsertStep(user, battle, UserBattleStep.PRE_VOTE);
        }

        UserBattleStep currentStep = status.step() == UserBattleStep.NONE
                ? UserBattleStep.PRE_VOTE
                : status.step();
        return new VoteResultResponse(vote.getId(), currentStep);
    }

    @Override
    @Transactional
    public VoteResultResponse postVote(Long battleId, Long userId, VoteRequest request) {
        Battle battle = battleService.findById(battleId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        BattleOption option = battleOptionRepository.findById(request.optionId())
                .orElseThrow(() -> new CustomException(ErrorCode.BATTLE_OPTION_NOT_FOUND));

        BattleVote vote = battleVoteRepository.findByBattleAndUser(battle, user)
                .orElseThrow(() -> new CustomException(ErrorCode.VOTE_NOT_FOUND));

        UserBattleStatusResponse status = userBattleService.getUserBattleStatus(user, battle);
        if (status.step() == UserBattleStep.NONE) {
            throw new CustomException(ErrorCode.PRE_VOTE_REQUIRED);
        }

        vote.doPostVote(option);
        userBattleService.upsertStep(user, battle, UserBattleStep.COMPLETED);
        eventPublisher.publishEvent(new VoteUpdatedEvent(battleId));

        // 오늘의 배틀(무료 진입)일 때만 사후 투표 완료 보상 +5P 지급
        if (!shouldChargeBattleEntryCredit(battle)) {
            creditService.addCredit(user.getId(), CreditType.BATTLE_VOTE, vote.getId());
        }

        return new VoteResultResponse(vote.getId(), UserBattleStep.COMPLETED);
    }

    @Override
    @Transactional
    public void deleteVotesByBattleId(Long battleId) {
        Battle battle = battleService.findById(battleId);
        List<BattleVote> votes = battleVoteRepository.findAllByBattle(battle);

        for (BattleVote vote : votes) {
            userBattleService.upsertStep(vote.getUser(), battle, UserBattleStep.NONE);
        }

        battleVoteRepository.deleteAllInBatch(votes);
    }

    @Override
    @Transactional
    public void completeTts(Long battleId, Long userId) {
        Battle battle = battleService.findById(battleId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        BattleVote vote = battleVoteRepository.findByBattleAndUser(battle, user)
                .orElseThrow(() -> new CustomException(ErrorCode.VOTE_NOT_FOUND));
        vote.completeTts();

        userBattleService.upsertStep(user, battle, UserBattleStep.POST_VOTE);
    }

    private boolean shouldChargeBattleEntryCredit(Battle battle) {
        LocalDate today = LocalDate.now(KST);
        return battle.getTargetDate() == null || !battle.getTargetDate().isEqual(today);
    }
}

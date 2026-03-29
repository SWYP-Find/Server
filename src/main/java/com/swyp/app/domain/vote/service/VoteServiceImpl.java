package com.swyp.app.domain.vote.service;

import com.swyp.app.domain.battle.entity.Battle;
import com.swyp.app.domain.battle.entity.BattleOption;
import com.swyp.app.domain.battle.repository.BattleOptionRepository;
import com.swyp.app.domain.battle.service.BattleService;
import com.swyp.app.domain.user.entity.User;
import com.swyp.app.domain.user.repository.UserRepository;
import com.swyp.app.domain.vote.converter.VoteConverter;
import com.swyp.app.domain.vote.dto.request.VoteRequest;
import com.swyp.app.domain.vote.dto.response.MyVoteResponse;
import com.swyp.app.domain.vote.dto.response.VoteResultResponse;
import com.swyp.app.domain.vote.dto.response.VoteStatsResponse;
import com.swyp.app.domain.vote.entity.Vote;
import com.swyp.app.domain.vote.enums.VoteStatus;
import com.swyp.app.domain.vote.repository.VoteRepository;
import com.swyp.app.global.common.exception.CustomException;
import com.swyp.app.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoteServiceImpl implements VoteService {

    private final VoteRepository voteRepository;
    private final BattleService battleService;
    private final BattleOptionRepository battleOptionRepository;
    private final UserRepository userRepository;

    @Override
    public BattleOption findPreVoteOption(Long battleId, Long userId) {
        Battle battle = battleService.findById(battleId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Vote vote = voteRepository.findByBattleAndUser(battle, user)
                .orElseThrow(() -> new CustomException(ErrorCode.VOTE_NOT_FOUND));

        if (vote.getPreVoteOption() == null) {
            throw new CustomException(ErrorCode.PRE_VOTE_REQUIRED);
        }
        return vote.getPreVoteOption();
    }

    @Override
    public Long findPostVoteOptionId(Long battleId, Long userId) {
        return voteRepository.findByBattleIdAndUserId(battleId, userId)
                .map(vote -> vote.getPostVoteOption() != null ? vote.getPostVoteOption().getId() : null)
                .orElse(null);
    }

    @Override
    public VoteStatsResponse getVoteStats(Long battleId) {
        Battle battle = battleService.findById(battleId);
        List<BattleOption> options = battleOptionRepository.findByBattle(battle);
        long totalCount = voteRepository.countByBattle(battle);

        List<VoteStatsResponse.OptionStat> stats = options.stream()
                .map(option -> {
                    long count = voteRepository.countByBattleAndPreVoteOption(battle, option);
                    double ratio = totalCount > 0
                            ? Math.round((double) count / totalCount * 1000.0) / 10.0
                            : 0.0;
                    return new VoteStatsResponse.OptionStat(
                            option.getId(), option.getLabel().name(), option.getTitle(), count, ratio);
                })
                .toList();

        LocalDateTime updatedAt = voteRepository.findTopByBattleOrderByUpdatedAtDesc(battle)
                .map(Vote::getUpdatedAt)
                .orElse(null);

        return VoteConverter.toVoteStatsResponse(stats, totalCount, updatedAt);
    }

    @Override
    public MyVoteResponse getMyVote(Long battleId, Long userId) {
        Battle battle = battleService.findById(battleId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Vote vote = voteRepository.findByBattleAndUser(battle, user)
                .orElseThrow(() -> new CustomException(ErrorCode.VOTE_NOT_FOUND));

        return VoteConverter.toMyVoteResponse(vote);
    }

    @Override
    @Transactional
    public VoteResultResponse preVote(Long battleId, Long userId, VoteRequest request) {
        Battle battle = battleService.findById(battleId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        BattleOption option = battleOptionRepository.findById(request.optionId())
                .orElseThrow(() -> new CustomException(ErrorCode.BATTLE_OPTION_NOT_FOUND));

        if (voteRepository.findByBattleAndUser(battle, user).isPresent()) {
            throw new CustomException(ErrorCode.VOTE_ALREADY_SUBMITTED);
        }

        Vote vote = Vote.createPreVote(user, battle, option);
        voteRepository.save(vote);

        return VoteConverter.toVoteResultResponse(vote);
    }

    @Override
    @Transactional
    public VoteResultResponse postVote(Long battleId, Long userId, VoteRequest request) {
        Battle battle = battleService.findById(battleId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        BattleOption option = battleOptionRepository.findById(request.optionId())
                .orElseThrow(() -> new CustomException(ErrorCode.BATTLE_OPTION_NOT_FOUND));

        Vote vote = voteRepository.findByBattleAndUser(battle, user)
                .orElseThrow(() -> new CustomException(ErrorCode.VOTE_NOT_FOUND));

        if (vote.getStatus() != VoteStatus.PRE_VOTED) {
            throw new CustomException(ErrorCode.INVALID_VOTE_STATUS);
        }

        vote.doPostVote(option);

        return VoteConverter.toVoteResultResponse(vote);
    }
}
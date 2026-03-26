package com.swyp.app.domain.vote.service;

import com.swyp.app.domain.battle.entity.Battle;
import com.swyp.app.domain.battle.entity.BattleOption;
import com.swyp.app.domain.battle.repository.BattleOptionRepository;
import com.swyp.app.domain.battle.service.BattleService;
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

    @Override
    public Long findPreVoteOptionId(Long battleId, Long userId) {
        Battle battle = battleService.findById(battleId);

        Vote vote = voteRepository.findByBattleAndUserId(battle, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.VOTE_NOT_FOUND));

        if (vote.getPreVoteOption() == null) {
            throw new CustomException(ErrorCode.PRE_VOTE_REQUIRED);
        }
        return vote.getPreVoteOption().getId();
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

        Vote vote = voteRepository.findByBattleAndUserId(battle, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.VOTE_NOT_FOUND));

        return VoteConverter.toMyVoteResponse(vote);
    }

    @Override
    @Transactional
    public VoteResultResponse preVote(Long battleId, Long userId, VoteRequest request) {
        Battle battle = battleService.findById(battleId);
        BattleOption option = battleOptionRepository.findById(request.optionId())
                .orElseThrow(() -> new CustomException(ErrorCode.BATTLE_OPTION_NOT_FOUND));

        // 이미 투표 내역이 존재하는지 검증
        if (voteRepository.findByBattleAndUserId(battle, userId).isPresent()) {
            throw new CustomException(ErrorCode.VOTE_ALREADY_SUBMITTED);
        }

        Vote vote = Vote.createPreVote(userId, battle, option);
        voteRepository.save(vote);

        return VoteConverter.toVoteResultResponse(vote);
    }

    @Override
    @Transactional
    public VoteResultResponse postVote(Long battleId, Long userId, VoteRequest request) {
        Battle battle = battleService.findById(battleId);
        BattleOption option = battleOptionRepository.findById(request.optionId())
                .orElseThrow(() -> new CustomException(ErrorCode.BATTLE_OPTION_NOT_FOUND));

        Vote vote = voteRepository.findByBattleAndUserId(battle, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.VOTE_NOT_FOUND));

        // 사전 투표 상태일 때만 사후 투표 가능
        if (vote.getStatus() != VoteStatus.PRE_VOTED) {
            throw new CustomException(ErrorCode.INVALID_VOTE_STATUS);
        }

        vote.doPostVote(option);

        return VoteConverter.toVoteResultResponse(vote);
    }
}
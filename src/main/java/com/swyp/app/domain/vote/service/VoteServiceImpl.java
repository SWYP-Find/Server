package com.swyp.app.domain.vote.service;

import com.swyp.app.domain.battle.entity.Battle;
import com.swyp.app.domain.battle.entity.BattleOption;
import com.swyp.app.domain.battle.repository.BattleOptionRepository;
import com.swyp.app.domain.battle.service.BattleService;
import com.swyp.app.domain.vote.dto.response.MyVoteResponse;
import com.swyp.app.domain.vote.dto.response.VoteStatsResponse;
import com.swyp.app.domain.vote.entity.Vote;
import com.swyp.app.domain.vote.repository.VoteRepository;
import com.swyp.app.global.common.exception.CustomException;
import com.swyp.app.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoteServiceImpl implements VoteService {

    private final VoteRepository voteRepository;
    private final BattleService battleService;
    private final BattleOptionRepository battleOptionRepository;

    @Override
    public UUID findPreVoteOptionId(UUID battleId, Long userId) {
        Battle battle = battleService.findById(battleId);
        Vote vote = voteRepository.findByBattleAndUserId(battle, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.VOTE_NOT_FOUND));
        if (vote.getPreVoteOption() == null) {
            throw new CustomException(ErrorCode.PERSPECTIVE_POST_VOTE_REQUIRED);
        }
        return vote.getPreVoteOption().getId();
    }

    @Override
    public VoteStatsResponse getVoteStats(UUID battleId) {
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

        return new VoteStatsResponse(stats, totalCount, updatedAt);
    }

    @Override
    public MyVoteResponse getMyVote(UUID battleId, Long userId) {
        Battle battle = battleService.findById(battleId);
        Vote vote = voteRepository.findByBattleAndUserId(battle, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.VOTE_NOT_FOUND));

        MyVoteResponse.OptionInfo preVote = toOptionInfo(vote.getPreVoteOption());
        MyVoteResponse.OptionInfo postVote = toOptionInfo(vote.getPostVoteOption());

        return new MyVoteResponse(preVote, postVote, vote.isMindChanged(), vote.getStatus());
    }

    private MyVoteResponse.OptionInfo toOptionInfo(BattleOption option) {
        if (option == null) return null;
        return new MyVoteResponse.OptionInfo(option.getId(), option.getLabel().name(), option.getTitle());
    }
}

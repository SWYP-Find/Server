package com.swyp.picke.domain.vote.converter;

import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.user.enums.UserBattleStep;
import com.swyp.picke.domain.vote.dto.response.MyVoteResponse;
import com.swyp.picke.domain.vote.dto.response.VoteResultResponse;
import com.swyp.picke.domain.vote.dto.response.VoteStatsResponse;
import com.swyp.picke.domain.vote.entity.BattleVote;
import java.time.LocalDateTime;
import java.util.List;

public class VoteConverter {

    public static VoteResultResponse toVoteResultResponse(BattleVote vote, UserBattleStep step) {
        return new VoteResultResponse(vote.getId(), step);
    }

    public static MyVoteResponse toMyVoteResponse(BattleVote vote, UserBattleStep step) {
        boolean opinionChanged = vote.getPreVoteOption() != null
                && vote.getPostVoteOption() != null
                && !vote.getPreVoteOption().getId().equals(vote.getPostVoteOption().getId());

        return new MyVoteResponse(
                vote.getBattle().getTitle(),
                toOptionInfo(vote.getPreVoteOption()),
                toOptionInfo(vote.getPostVoteOption()),
                step,
                opinionChanged
        );
    }

    public static VoteStatsResponse toVoteStatsResponse(
            List<VoteStatsResponse.OptionStat> stats,
            long totalCount,
            LocalDateTime updatedAt
    ) {
        return new VoteStatsResponse(stats, totalCount, updatedAt);
    }

    private static MyVoteResponse.OptionInfo toOptionInfo(BattleOption option) {
        if (option == null) {
            return null;
        }
        return new MyVoteResponse.OptionInfo(option.getId(), option.getLabel().name(), option.getTitle());
    }
}
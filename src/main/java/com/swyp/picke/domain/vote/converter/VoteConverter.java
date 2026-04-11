package com.swyp.picke.domain.vote.converter;

import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.user.enums.UserBattleStep;
import com.swyp.picke.domain.vote.dto.response.MyVoteResponse;
import com.swyp.picke.domain.vote.dto.response.VoteResultResponse;
import com.swyp.picke.domain.vote.dto.response.VoteStatsResponse;
import com.swyp.picke.domain.vote.entity.Vote;

import java.time.LocalDateTime;
import java.util.List;

public class VoteConverter {

    // [수정] UserBattleStep을 인자로 받도록 변경
    public static VoteResultResponse toVoteResultResponse(Vote vote, UserBattleStep step) {
        return new VoteResultResponse(vote.getId(), step);
    }

    // [수정] UserBattleStep을 인자로 받아 MyVoteResponse의 status 필드에 매핑
    public static MyVoteResponse toMyVoteResponse(Vote vote, UserBattleStep step) {
        boolean opinionChanged = vote.getPreVoteOption() != null
                && vote.getPostVoteOption() != null
                && !vote.getPreVoteOption().getId().equals(vote.getPostVoteOption().getId());

        return new MyVoteResponse(
                vote.getBattle().getTitle(),
                toOptionInfo(vote.getPreVoteOption()),
                toOptionInfo(vote.getPostVoteOption()),
                step, // 외부에서 넘겨받은 UserBattleStep 사용
                opinionChanged
        );
    }

    // 투표 통계 변환
    public static VoteStatsResponse toVoteStatsResponse(List<VoteStatsResponse.OptionStat> stats, long totalCount, LocalDateTime updatedAt) {
        return new VoteStatsResponse(stats, totalCount, updatedAt);
    }

    // 옵션 정보를 응답용으로 변환 (null 안전 처리)
    private static MyVoteResponse.OptionInfo toOptionInfo(BattleOption option) {
        if (option == null) return null;
        return new MyVoteResponse.OptionInfo(option.getId(), option.getLabel().name(), option.getTitle());
    }
}
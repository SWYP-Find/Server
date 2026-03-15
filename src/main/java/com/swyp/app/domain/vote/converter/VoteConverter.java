package com.swyp.app.domain.vote.converter;

import com.swyp.app.domain.battle.entity.BattleOption;
import com.swyp.app.domain.vote.dto.response.MyVoteResponse;
import com.swyp.app.domain.vote.dto.response.VoteResultResponse;
import com.swyp.app.domain.vote.dto.response.VoteStatsResponse;
import com.swyp.app.domain.vote.entity.Vote;

import java.time.LocalDateTime;
import java.util.List;

public class VoteConverter {

    // 투표 실행 결과 변환
    public static VoteResultResponse toVoteResultResponse(Vote vote) {
        return new VoteResultResponse(vote.getId(), vote.getStatus());
    }

    // 내 투표 내역 변환
    public static MyVoteResponse toMyVoteResponse(Vote vote) {
        return new MyVoteResponse(
                toOptionInfo(vote.getPreVoteOption()),
                toOptionInfo(vote.getPostVoteOption()),
                vote.getStatus()
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
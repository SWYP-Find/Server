package com.swyp.picke.domain.vote.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record VoteStatsResponse(
        List<OptionStat> options,
        long totalCount,
        LocalDateTime updatedAt
) {
    public record OptionStat(
            Long optionId,
            String title,
            String imageUrl,
            long voteCount,
            double ratio
    ) {}
}

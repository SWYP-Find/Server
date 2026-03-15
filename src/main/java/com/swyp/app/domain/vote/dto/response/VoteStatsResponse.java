package com.swyp.app.domain.vote.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record VoteStatsResponse(
        List<OptionStat> options,
        long totalCount,
        LocalDateTime updatedAt
) {
    public record OptionStat(
            UUID optionId,
            String label,
            String title,
            long voteCount,
            double ratio
    ) {}
}

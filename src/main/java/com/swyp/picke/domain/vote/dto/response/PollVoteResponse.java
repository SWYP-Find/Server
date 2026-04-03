package com.swyp.picke.domain.vote.dto.response;

import java.util.List;

public record PollVoteResponse(
        Long battleId,
        Long selectedOptionId,
        long totalCount,
        List<OptionStat> stats
) {
    public record OptionStat(Long optionId, String label, String title, long voteCount, double ratio) {}
}

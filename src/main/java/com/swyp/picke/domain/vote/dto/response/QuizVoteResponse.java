package com.swyp.picke.domain.vote.dto.response;

import java.util.List;

public record QuizVoteResponse(
        Long battleId,
        Long selectedOptionId,
        long totalCount,
        List<OptionStat> stats
) {
    public record OptionStat(Long optionId, String label, String title, Boolean isCorrect, long voteCount, double ratio) {}
}

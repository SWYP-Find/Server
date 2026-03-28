package com.swyp.app.domain.home.dto.response;

import java.util.List;

public record HomeTodayVoteResponse(
        Long battleId,
        String titlePrefix,
        String titleSuffix,
        String summary,
        Long participantsCount,
        List<HomeTodayVoteOptionResponse> options
) {}

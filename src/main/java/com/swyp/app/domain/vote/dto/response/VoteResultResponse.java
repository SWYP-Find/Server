package com.swyp.app.domain.vote.dto.response;

import com.swyp.app.domain.vote.enums.VoteStatus;

public record VoteResultResponse(
        Long voteId,
        VoteStatus status
) {}
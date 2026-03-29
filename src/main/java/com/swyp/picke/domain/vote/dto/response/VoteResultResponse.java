package com.swyp.picke.domain.vote.dto.response;

import com.swyp.picke.domain.vote.enums.VoteStatus;

public record VoteResultResponse(
        Long voteId,
        VoteStatus status
) {}
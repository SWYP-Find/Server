package com.swyp.app.domain.vote.dto.response;

import com.swyp.app.domain.vote.enums.VoteStatus;
import java.util.UUID;

public record VoteResultResponse(
        UUID voteId,
        VoteStatus status
) {}
package com.swyp.app.domain.vote.dto.request;

import java.util.UUID;

public record VoteRequest(
        UUID optionId
) {}
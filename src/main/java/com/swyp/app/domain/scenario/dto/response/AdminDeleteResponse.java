package com.swyp.app.domain.scenario.dto.response;

import java.time.LocalDateTime;

public record AdminDeleteResponse(
        boolean success,
        LocalDateTime deletedAt
) {}

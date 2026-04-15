package com.swyp.picke.domain.admin.dto.scenario.response;

import java.time.LocalDateTime;

public record AdminDeleteResponse(
        boolean success,
        LocalDateTime deletedAt
) {}


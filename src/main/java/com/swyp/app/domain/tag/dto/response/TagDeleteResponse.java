package com.swyp.app.domain.tag.dto.response;

import java.time.LocalDateTime;

public record TagDeleteResponse(
        boolean success,
        LocalDateTime deletedAt
) {}
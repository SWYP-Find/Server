package com.swyp.app.domain.perspective.dto.response;

import com.swyp.app.domain.perspective.entity.PerspectiveStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record MyPerspectiveResponse(
        UUID perspectiveId,
        String content,
        PerspectiveStatus status,
        LocalDateTime createdAt
) {}

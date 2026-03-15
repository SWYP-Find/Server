package com.swyp.app.domain.perspective.dto.response;

import com.swyp.app.domain.perspective.entity.PerspectiveStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreatePerspectiveResponse(
        UUID perspectiveId,
        PerspectiveStatus status,
        LocalDateTime createdAt
) {}

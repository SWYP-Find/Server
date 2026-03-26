package com.swyp.app.domain.perspective.dto.response;

import com.swyp.app.domain.perspective.enums.PerspectiveStatus;

import java.time.LocalDateTime;

public record CreatePerspectiveResponse(
        Long perspectiveId,
        PerspectiveStatus status,
        LocalDateTime createdAt
) {}

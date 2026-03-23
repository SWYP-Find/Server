package com.swyp.app.domain.perspective.dto.response;

import com.swyp.app.domain.perspective.enums.PerspectiveStatus;

import java.time.LocalDateTime;

public record MyPerspectiveResponse(
        Long perspectiveId,
        String content,
        PerspectiveStatus status,
        LocalDateTime createdAt
) {}

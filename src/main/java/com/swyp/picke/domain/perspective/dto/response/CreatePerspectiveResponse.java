package com.swyp.picke.domain.perspective.dto.response;

import com.swyp.picke.domain.perspective.enums.PerspectiveStatus;

import java.time.LocalDateTime;

public record CreatePerspectiveResponse(
        Long perspectiveId,
        PerspectiveStatus status,
        LocalDateTime createdAt
) {}

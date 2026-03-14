package com.swyp.app.domain.perspective.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record UpdatePerspectiveResponse(
        UUID perspectiveId,
        String content,
        LocalDateTime updatedAt
) {}

package com.swyp.app.domain.perspective.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record UpdateCommentResponse(
        UUID commentId,
        String content,
        LocalDateTime updatedAt
) {}

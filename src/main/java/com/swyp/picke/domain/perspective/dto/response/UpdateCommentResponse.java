package com.swyp.picke.domain.perspective.dto.response;

import java.time.LocalDateTime;

public record UpdateCommentResponse(
        Long commentId,
        String content,
        LocalDateTime updatedAt
) {}

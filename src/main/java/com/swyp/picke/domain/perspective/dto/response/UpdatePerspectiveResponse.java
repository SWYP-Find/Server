package com.swyp.picke.domain.perspective.dto.response;

import java.time.LocalDateTime;

public record UpdatePerspectiveResponse(
        Long perspectiveId,
        String content,
        LocalDateTime updatedAt
) {}

package com.swyp.app.domain.tag.dto.response;

import com.swyp.app.domain.tag.enums.TagType;
import java.time.LocalDateTime;
import java.util.UUID;

public record TagResponse(
        UUID tagId,
        String name,
        TagType type,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
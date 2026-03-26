package com.swyp.app.domain.tag.dto.response;

import com.swyp.app.domain.tag.enums.TagType;
import java.time.LocalDateTime;

public record TagResponse(
        Long tagId,
        String name,
        TagType type,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
package com.swyp.picke.domain.admin.dto.tag.response;

import com.swyp.picke.domain.tag.enums.TagType;
import java.time.LocalDateTime;

public record TagResponse(
        Long tagId,
        String name,
        TagType type,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
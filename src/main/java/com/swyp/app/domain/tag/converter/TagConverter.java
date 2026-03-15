package com.swyp.app.domain.tag.converter;

import com.swyp.app.domain.tag.dto.request.TagRequest;
import com.swyp.app.domain.tag.dto.response.*;
import com.swyp.app.domain.tag.entity.Tag;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class TagConverter {

    public static Tag toEntity(TagRequest request) {
        return Tag.builder()
                .name(request.name())
                .type(request.type())
                .build();
    }

    public static TagResponse toDetailResponse(Tag tag) {
        return new TagResponse(tag.getId(), tag.getName(), tag.getType(), tag.getCreatedAt(), tag.getUpdatedAt());
    }

    public static TagListResponse toListResponse(List<Tag> tags) {
        List<TagResponse> items = tags.stream()
                .map(TagConverter::toDetailResponse)
                .toList();
        return new TagListResponse(items, items.size());
    }

    public static TagDeleteResponse toDeleteResponse() {
        return new TagDeleteResponse(true, LocalDateTime.now());
    }
}
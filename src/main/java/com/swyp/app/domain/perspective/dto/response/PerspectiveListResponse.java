package com.swyp.app.domain.perspective.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PerspectiveListResponse(
        List<Item> items,
        String nextCursor,
        boolean hasNext
) {
    public record Item(
            UUID perspectiveId,
            UserSummary user,
            OptionSummary option,
            String content,
            int likeCount,
            int commentCount,
            boolean isLiked,
            LocalDateTime createdAt
    ) {}

    public record UserSummary(
            String userTag,
            String nickname,
            String characterUrl
    ) {}

    public record OptionSummary(
            UUID optionId,
            String label,
            String title
    ) {}
}

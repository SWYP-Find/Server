package com.swyp.app.domain.perspective.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CommentListResponse(
        List<Item> items,
        String nextCursor,
        boolean hasNext
) {
    public record Item(
            UUID commentId,
            UserSummary user,
            String content,
            boolean isMine,
            LocalDateTime createdAt
    ) {}

    public record UserSummary(String userTag, String nickname, String characterType) {}
}

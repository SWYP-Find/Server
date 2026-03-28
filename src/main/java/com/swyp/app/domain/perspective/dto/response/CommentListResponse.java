package com.swyp.app.domain.perspective.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record CommentListResponse(
        List<Item> items,
        String nextCursor,
        boolean hasNext
) {
    public record Item(
            Long commentId,
            UserSummary user,
            String stance,
            String content,
            int likeCount,
            boolean isLiked,
            boolean isMine,
            LocalDateTime createdAt
    ) {}

    public record UserSummary(String userTag, String nickname, String characterType, String characterImageUrl) {}
}

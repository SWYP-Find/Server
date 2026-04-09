package com.swyp.picke.domain.perspective.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record CommentListResponse(
        List<Item> items,
        String nextCursor,
        boolean hasNext
) {
    @Schema(name = "CommentItem")
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

    @Schema(name = "CommentUserSummary")
    public record UserSummary(String userTag, String nickname, String characterType, String characterImageUrl) {}
}

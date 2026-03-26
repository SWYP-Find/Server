package com.swyp.app.domain.perspective.dto.response;

import java.time.LocalDateTime;

public record CreateCommentResponse(
        Long commentId,
        UserSummary user,
        String content,
        LocalDateTime createdAt
) {
    public record UserSummary(String userTag, String nickname, String characterType) {}
}

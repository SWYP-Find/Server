package com.swyp.app.domain.perspective.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateCommentResponse(
        UUID commentId,
        UserSummary user,
        String content,
        LocalDateTime createdAt
) {
    public record UserSummary(String userTag, String nickname, String characterType) {}
}

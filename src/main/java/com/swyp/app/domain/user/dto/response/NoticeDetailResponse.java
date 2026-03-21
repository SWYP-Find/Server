package com.swyp.app.domain.user.dto.response;

import com.swyp.app.domain.notice.entity.NoticeType;

import java.time.LocalDateTime;
import java.util.UUID;

public record NoticeDetailResponse(
        UUID noticeId,
        NoticeType type,
        String title,
        String body,
        boolean isPinned,
        LocalDateTime publishedAt
) {
}

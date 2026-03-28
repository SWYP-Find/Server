package com.swyp.app.domain.user.dto.response;

import com.swyp.app.domain.notice.enums.NoticeType;

import java.time.LocalDateTime;

public record NoticeDetailResponse(
        Long noticeId,
        NoticeType type,
        String title,
        String body,
        boolean isPinned,
        LocalDateTime publishedAt
) {
}

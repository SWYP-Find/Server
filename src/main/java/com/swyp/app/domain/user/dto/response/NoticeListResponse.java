package com.swyp.app.domain.user.dto.response;

import com.swyp.app.domain.notice.entity.NoticeType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record NoticeListResponse(
        List<NoticeItem> items
) {

    public record NoticeItem(
            UUID noticeId,
            NoticeType type,
            String title,
            String bodyPreview,
            boolean isPinned,
            LocalDateTime publishedAt
    ) {
    }
}

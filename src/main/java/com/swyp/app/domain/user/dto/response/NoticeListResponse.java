package com.swyp.app.domain.user.dto.response;

import com.swyp.app.domain.notice.enums.NoticeType;

import java.time.LocalDateTime;
import java.util.List;

public record NoticeListResponse(
        List<NoticeItem> items
) {

    public record NoticeItem(
            Long noticeId,
            NoticeType type,
            String title,
            String bodyPreview,
            boolean isPinned,
            LocalDateTime publishedAt
    ) {
    }
}

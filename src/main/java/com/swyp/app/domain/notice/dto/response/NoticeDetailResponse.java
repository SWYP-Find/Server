package com.swyp.app.domain.notice.dto.response;

import com.swyp.app.domain.notice.enums.NoticePlacement;
import com.swyp.app.domain.notice.enums.NoticeType;

import java.time.LocalDateTime;

public record NoticeDetailResponse(
        Long noticeId,
        String title,
        String body,
        NoticeType type,
        NoticePlacement placement,
        boolean pinned,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        LocalDateTime createdAt
) {
}

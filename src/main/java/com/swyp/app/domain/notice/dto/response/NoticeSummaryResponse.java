package com.swyp.app.domain.notice.dto.response;

import com.swyp.app.domain.notice.entity.NoticePlacement;
import com.swyp.app.domain.notice.entity.NoticeType;

import java.time.LocalDateTime;
import java.util.UUID;

public record NoticeSummaryResponse(
        UUID noticeId,
        String title,
        String body,
        NoticeType type,
        NoticePlacement placement,
        boolean pinned,
        LocalDateTime startsAt,
        LocalDateTime endsAt
) {
}

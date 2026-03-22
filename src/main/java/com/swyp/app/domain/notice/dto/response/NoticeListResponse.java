package com.swyp.app.domain.notice.dto.response;

import java.util.List;

public record NoticeListResponse(
        List<NoticeSummaryResponse> items,
        int totalCount
) {
}

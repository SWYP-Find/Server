package com.swyp.picke.domain.admin.dto.notification.response;

import java.util.List;

public record AdminNoticeListResponse(
        List<AdminNoticeSummaryResponse> items,
        boolean hasNext
) {}

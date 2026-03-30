package com.swyp.picke.domain.notification.dto.response;

import java.util.List;

public record NotificationListResponse(
        List<NotificationSummaryResponse> items,
        boolean hasNext
) {}

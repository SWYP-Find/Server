package com.swyp.app.domain.notification.dto.response;

import java.util.List;

public record NotificationListResponse(
        List<NotificationSummaryResponse> items,
        boolean hasNext
) {}

package com.swyp.app.domain.notification.dto.response;

import com.swyp.app.domain.notification.enums.NotificationCategory;
import com.swyp.app.domain.notification.enums.NotificationDetailCode;

import java.time.LocalDateTime;

public record NotificationSummaryResponse(
        Long notificationId,
        NotificationCategory category,
        int detailCode,
        String title,
        String body,
        Long referenceId,
        boolean isRead,
        LocalDateTime createdAt
) {}

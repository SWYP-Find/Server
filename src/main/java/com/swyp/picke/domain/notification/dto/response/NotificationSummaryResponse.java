package com.swyp.picke.domain.notification.dto.response;

import com.swyp.picke.domain.notification.enums.NotificationCategory;

import java.time.LocalDateTime;

public record NotificationSummaryResponse(
        Long notificationId,
        NotificationCategory category,
        String detailCode,
        String title,
        String body,
        Long referenceId,
        boolean isRead,
        LocalDateTime createdAt
) {}

package com.swyp.picke.domain.admin.dto.notification.response;

import com.swyp.picke.domain.notification.enums.NotificationCategory;

import java.time.LocalDateTime;

public record AdminNoticeSummaryResponse(
        Long notificationId,
        NotificationCategory category,
        String detailCode,
        String title,
        String body,
        Long referenceId,
        LocalDateTime createdAt
) {}

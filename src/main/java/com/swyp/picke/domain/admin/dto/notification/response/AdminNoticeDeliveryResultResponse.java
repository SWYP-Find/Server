package com.swyp.picke.domain.admin.dto.notification.response;

public record AdminNoticeDeliveryResultResponse(
        Long notificationId,
        int targetCount,
        int successCount,
        int failureCount,
        boolean pending
) {}

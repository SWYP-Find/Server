package com.swyp.picke.domain.admin.dto.notification.response;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record AdminNotificationScheduleResponse(
        Long id,
        String title,
        String subtitle,
        LocalTime sendTime,
        boolean enabled,
        LocalDateTime createdAt
) {}

package com.swyp.picke.domain.admin.dto.notification.request;

import jakarta.validation.constraints.NotNull;

public record AdminNotificationScheduleTestRequest(
        @NotNull Long userId
) {}

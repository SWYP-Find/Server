package com.swyp.picke.domain.admin.dto.notification.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record AdminNotificationScheduleRequest(
        @NotBlank String title,
        @NotBlank String subtitle,
        @NotNull LocalTime sendTime,
        @NotNull Boolean enabled
) {}

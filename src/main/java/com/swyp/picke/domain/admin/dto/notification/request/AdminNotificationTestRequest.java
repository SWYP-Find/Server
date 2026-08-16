package com.swyp.picke.domain.admin.dto.notification.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminNotificationTestRequest(
        @NotNull Long userId,
        @NotBlank String title,
        @NotBlank String body
) {}

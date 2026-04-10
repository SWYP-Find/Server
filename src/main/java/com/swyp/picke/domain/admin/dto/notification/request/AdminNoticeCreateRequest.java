package com.swyp.picke.domain.admin.dto.notification.request;

import com.swyp.picke.domain.notification.enums.NotificationCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminNoticeCreateRequest(
        @NotNull NotificationCategory category,
        @NotBlank String title,
        @NotBlank String body
) {}

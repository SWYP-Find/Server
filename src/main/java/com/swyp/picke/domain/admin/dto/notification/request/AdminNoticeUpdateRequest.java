package com.swyp.picke.domain.admin.dto.notification.request;

import jakarta.validation.constraints.NotBlank;

public record AdminNoticeUpdateRequest(
        @NotBlank String title,
        @NotBlank String body
) {}

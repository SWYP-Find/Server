package com.swyp.picke.domain.notification.dto.request;

import com.swyp.picke.domain.notification.enums.DevicePlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterDeviceRequest(
        @NotBlank
        String fcmToken,

        @NotNull
        DevicePlatform platform
) {}

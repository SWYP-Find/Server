package com.swyp.picke.domain.notification.dto.request;

import com.swyp.picke.domain.notification.enums.DevicePlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 푸시 토큰 등록 요청.
 * platform=ANDROID이면 FCM 토큰, platform=IOS이면 APNs 디바이스 토큰을 fcmToken에 담아 전달한다.
 */
public record RegisterDeviceRequest(
        @NotBlank
        String fcmToken,

        @NotNull
        DevicePlatform platform
) {}

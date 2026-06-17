package com.swyp.picke.domain.notification.controller;

import com.swyp.picke.domain.notification.dto.request.RegisterDeviceRequest;
import com.swyp.picke.domain.notification.service.DeviceService;
import com.swyp.picke.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "디바이스 API", description = "푸시 알림을 위한 디바이스 토큰 등록/해제 (Android: FCM 토큰, iOS: APNs 디바이스 토큰)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/devices")
public class DeviceController {

    private final DeviceService deviceService;

    @Operation(summary = "디바이스 푸시 토큰 등록", description = "로그인 또는 토큰 갱신 시 푸시 토큰을 등록합니다. Android는 FCM 토큰, iOS는 APNs 디바이스 토큰을 전달합니다.")
    @PostMapping
    public ApiResponse<Void> registerDevice(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid RegisterDeviceRequest request
    ) {
        deviceService.registerDevice(userId, request);
        return ApiResponse.onSuccess(null);
    }

    @Operation(summary = "디바이스 푸시 토큰 해제", description = "로그아웃 시 푸시 토큰을 해제합니다.")
    @DeleteMapping
    public ApiResponse<Void> unregisterDevice(
            @AuthenticationPrincipal Long userId,
            @RequestParam String fcmToken
    ) {
        deviceService.unregisterDevice(userId, fcmToken);
        return ApiResponse.onSuccess(null);
    }
}

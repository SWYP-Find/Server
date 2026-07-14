package com.swyp.picke.domain.notification.controller;

import com.swyp.picke.domain.notification.enums.NotificationDetailCode;
import com.swyp.picke.domain.notification.service.NotificationService;
import com.swyp.picke.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "[Test] Notification", description = "인앱 알림 테스트 API")
@RestController
@RequestMapping("/api/test/notifications")
@RequiredArgsConstructor
public class NotificationTestController {

    private final NotificationService notificationService;

    @Operation(summary = "인앱 알림 즉시 생성", description = "실제 푸시 발송 없이 지정한 유저에게 인앱 알림을 즉시 생성합니다.")
    @PostMapping
    public ApiResponse<String> createTestNotification(
            @RequestParam Long userId,
            @RequestParam NotificationDetailCode detailCode,
            @RequestParam String body,
            @RequestParam Long referenceId
    ) {
        notificationService.createNotification(userId, detailCode, body, referenceId);
        return ApiResponse.onSuccess("userId=" + userId + " 알림 생성 완료 (detailCode=" + detailCode + ")");
    }
}

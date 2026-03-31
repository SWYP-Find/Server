package com.swyp.picke.domain.notification.controller;

import com.swyp.picke.domain.notification.dto.response.NotificationDetailResponse;
import com.swyp.picke.domain.notification.dto.response.NotificationListResponse;
import com.swyp.picke.domain.notification.enums.NotificationCategory;
import com.swyp.picke.domain.notification.service.NotificationService;
import com.swyp.picke.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "알림 API", description = "알림 조회 및 읽음 처리")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "알림 목록 조회")
    @GetMapping
    public ApiResponse<NotificationListResponse> getNotifications(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) NotificationCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.onSuccess(notificationService.getNotifications(userId, category, page, size));
    }

    @Operation(summary = "알림 상세 조회")
    @GetMapping("/{notificationId}")
    public ApiResponse<NotificationDetailResponse> getNotificationDetail(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long notificationId
    ) {
        return ApiResponse.onSuccess(notificationService.getNotificationDetail(userId, notificationId));
    }

    @Operation(summary = "알림 개별 읽음 처리")
    @PatchMapping("/{notificationId}/read")
    public ApiResponse<Void> markAsRead(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long notificationId
    ) {
        notificationService.markAsRead(userId, notificationId);
        return ApiResponse.onSuccess(null);
    }

    @Operation(summary = "알림 전체 읽음 처리")
    @PatchMapping("/read-all")
    public ApiResponse<Void> markAllAsRead(@AuthenticationPrincipal Long userId) {
        notificationService.markAllAsRead(userId);
        return ApiResponse.onSuccess(null);
    }
}

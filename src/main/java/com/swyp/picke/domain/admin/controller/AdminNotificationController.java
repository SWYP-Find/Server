package com.swyp.picke.domain.admin.controller;

import com.swyp.picke.domain.admin.dto.notification.request.AdminNoticeCreateRequest;
import com.swyp.picke.domain.admin.dto.notification.request.AdminNoticeUpdateRequest;
import com.swyp.picke.domain.admin.dto.notification.request.AdminNotificationTestRequest;
import com.swyp.picke.domain.admin.dto.notification.response.AdminNoticeDetailResponse;
import com.swyp.picke.domain.admin.dto.notification.response.AdminNoticeListResponse;
import com.swyp.picke.domain.admin.service.AdminNotificationService;
import com.swyp.picke.domain.notification.enums.NotificationCategory;
import com.swyp.picke.domain.notification.service.NotificationDispatchService;
import com.swyp.picke.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 공지 API", description = "공지사항/이벤트 작성 및 조회")
@RestController
@RequestMapping("/api/v1/admin/notices")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminNotificationController {

    private final AdminNotificationService adminNotificationService;
    private final NotificationDispatchService notificationDispatchService;

    @Operation(summary = "공지사항 작성")
    @PostMapping
    public ApiResponse<AdminNoticeDetailResponse> createNotice(
            @RequestBody @Valid AdminNoticeCreateRequest request
    ) {
        return ApiResponse.onSuccess(adminNotificationService.createNotice(request));
    }

    @Operation(summary = "공지사항 목록 조회")
    @GetMapping
    public ApiResponse<AdminNoticeListResponse> getNotices(
            @RequestParam(required = false) NotificationCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.onSuccess(adminNotificationService.getNotices(category, page, size));
    }

    @Operation(summary = "공지사항 상세 조회")
    @GetMapping("/{noticeId}")
    public ApiResponse<AdminNoticeDetailResponse> getNoticeDetail(@PathVariable Long noticeId) {
        return ApiResponse.onSuccess(adminNotificationService.getNoticeDetail(noticeId));
    }

    @Operation(summary = "공지사항 수정", description = "이미 발송된 알림함/푸시는 재발송되지 않으며, 알림함에 남는 텍스트만 갱신된다.")
    @PutMapping("/{noticeId}")
    public ApiResponse<AdminNoticeDetailResponse> updateNotice(
            @PathVariable Long noticeId,
            @RequestBody @Valid AdminNoticeUpdateRequest request
    ) {
        return ApiResponse.onSuccess(adminNotificationService.updateNotice(noticeId, request));
    }

    @Operation(summary = "공지사항 삭제")
    @DeleteMapping("/{noticeId}")
    public ApiResponse<Void> deleteNotice(@PathVariable Long noticeId) {
        adminNotificationService.deleteNotice(noticeId);
        return ApiResponse.onSuccess(null);
    }

    @Operation(summary = "푸시 알림 발송 테스트", description = "특정 유저의 등록된 디바이스로 알림 설정(ON/OFF) 무관하게 즉시 테스트 푸시를 발송한다.")
    @PostMapping("/test")
    public ApiResponse<Void> sendTestPush(
            @RequestBody @Valid AdminNotificationTestRequest request
    ) {
        notificationDispatchService.sendTestPush(request.userId(), request.title(), request.body());
        return ApiResponse.onSuccess(null);
    }
}

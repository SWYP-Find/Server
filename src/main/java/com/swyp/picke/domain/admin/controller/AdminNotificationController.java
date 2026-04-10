package com.swyp.picke.domain.admin.controller;

import com.swyp.picke.domain.admin.dto.notification.request.AdminNoticeCreateRequest;
import com.swyp.picke.domain.admin.dto.notification.response.AdminNoticeDetailResponse;
import com.swyp.picke.domain.admin.dto.notification.response.AdminNoticeListResponse;
import com.swyp.picke.domain.admin.service.AdminNotificationService;
import com.swyp.picke.domain.notification.enums.NotificationCategory;
import com.swyp.picke.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
}

package com.swyp.picke.domain.admin.controller;

import com.swyp.picke.domain.admin.dto.notification.request.AdminNotificationScheduleRequest;
import com.swyp.picke.domain.admin.dto.notification.request.AdminNotificationScheduleToggleRequest;
import com.swyp.picke.domain.admin.dto.notification.response.AdminNotificationScheduleListResponse;
import com.swyp.picke.domain.admin.dto.notification.response.AdminNotificationScheduleResponse;
import com.swyp.picke.domain.admin.service.AdminNotificationScheduleService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 예약 알림 API", description = "매일 정해진 시각에 자동 발송되는 푸시알림 예약 관리")
@RestController
@RequestMapping("/api/v1/admin/notification-schedules")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminNotificationScheduleController {

    private final AdminNotificationScheduleService adminNotificationScheduleService;

    @Operation(summary = "예약 알림 등록")
    @PostMapping
    public ApiResponse<AdminNotificationScheduleResponse> create(
            @RequestBody @Valid AdminNotificationScheduleRequest request
    ) {
        return ApiResponse.onSuccess(adminNotificationScheduleService.create(request));
    }

    @Operation(summary = "예약 알림 목록 조회")
    @GetMapping
    public ApiResponse<AdminNotificationScheduleListResponse> getSchedules() {
        return ApiResponse.onSuccess(adminNotificationScheduleService.getSchedules());
    }

    @Operation(summary = "예약 알림 상세 조회")
    @GetMapping("/{scheduleId}")
    public ApiResponse<AdminNotificationScheduleResponse> getSchedule(@PathVariable Long scheduleId) {
        return ApiResponse.onSuccess(adminNotificationScheduleService.getSchedule(scheduleId));
    }

    @Operation(summary = "예약 알림 수정")
    @PutMapping("/{scheduleId}")
    public ApiResponse<AdminNotificationScheduleResponse> update(
            @PathVariable Long scheduleId,
            @RequestBody @Valid AdminNotificationScheduleRequest request
    ) {
        return ApiResponse.onSuccess(adminNotificationScheduleService.update(scheduleId, request));
    }

    @Operation(summary = "예약 알림 On/Off 전환")
    @PatchMapping("/{scheduleId}/toggle")
    public ApiResponse<AdminNotificationScheduleResponse> toggle(
            @PathVariable Long scheduleId,
            @RequestBody @Valid AdminNotificationScheduleToggleRequest request
    ) {
        return ApiResponse.onSuccess(adminNotificationScheduleService.toggle(scheduleId, request));
    }

    @Operation(summary = "예약 알림 삭제")
    @DeleteMapping("/{scheduleId}")
    public ApiResponse<Void> delete(@PathVariable Long scheduleId) {
        adminNotificationScheduleService.delete(scheduleId);
        return ApiResponse.onSuccess(null);
    }
}

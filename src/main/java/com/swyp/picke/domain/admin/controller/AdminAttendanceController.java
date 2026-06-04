package com.swyp.picke.domain.admin.controller;

import com.swyp.picke.domain.admin.dto.attendance.response.AdminAttendanceResponse.StatsResponse;
import com.swyp.picke.domain.admin.dto.attendance.response.AdminAttendanceResponse.UserAttendanceResponse;
import com.swyp.picke.domain.admin.service.AdminAttendanceService;
import com.swyp.picke.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "관리자 출석체크 API", description = "출석 현황 집계 및 특정 유저 출석 이력 조회")
@RestController
@RequestMapping("/api/v1/admin/attendance")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAttendanceController {

    private final AdminAttendanceService adminAttendanceService;

    @Operation(summary = "전체 출석 현황 집계", description = "일별 출석자 수, 출석률을 집계합니다. 기본 최근 7일, 최대 90일.")
    @GetMapping("/stats")
    public ApiResponse<StatsResponse> getStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ApiResponse.onSuccess(adminAttendanceService.getStats(from, to));
    }

    @Operation(summary = "특정 유저 출석 이력 조회", description = "user_tag로 특정 유저의 출석 통계 및 이력을 조회합니다.")
    @GetMapping("/users/{userTag}")
    public ApiResponse<UserAttendanceResponse> getUserAttendance(
            @PathVariable String userTag,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.onSuccess(adminAttendanceService.getUserAttendance(userTag, page, size));
    }
}

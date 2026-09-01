package com.swyp.picke.domain.admin.controller;

import com.swyp.picke.domain.admin.dto.dashboard.response.AdminDashboardDauMauResponse;
import com.swyp.picke.domain.admin.dto.dashboard.response.AdminDashboardSummaryResponse;
import com.swyp.picke.domain.admin.service.AdminDashboardService;
import com.swyp.picke.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 대시보드 API", description = "가입/활동 지표 등 어드민 대시보드 조회")
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @Operation(summary = "오늘 요약 카드 조회", description = "오늘 기준 신규가입/로그인유저/활동유저(DAU) 수와 현재 전체 유저 수를 조회한다.")
    @GetMapping("/summary")
    public ApiResponse<AdminDashboardSummaryResponse> getSummary() {
        return ApiResponse.onSuccess(adminDashboardService.getSummary());
    }

    @Operation(summary = "DAU/MAU 추이 조회", description = "granularity=day면 일자별 DAU, month면 그날 기준 최근 30일 롤링 윈도우의 MAU를 반환한다.")
    @GetMapping("/dau-mau")
    public ApiResponse<AdminDashboardDauMauResponse> getDauMauTrend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "day") String granularity
    ) {
        return ApiResponse.onSuccess(adminDashboardService.getDauMauTrend(from, to, granularity));
    }
}

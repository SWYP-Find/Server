package com.swyp.picke.domain.admin.controller;

import com.swyp.picke.domain.admin.dto.dashboard.response.AdminDashboardAttendanceStatsResponse;
import com.swyp.picke.domain.admin.dto.dashboard.response.AdminDashboardBattleStatsResponse;
import com.swyp.picke.domain.admin.dto.dashboard.response.AdminDashboardCreditStatsResponse;
import com.swyp.picke.domain.admin.dto.dashboard.response.AdminDashboardDauMauResponse;
import com.swyp.picke.domain.admin.dto.dashboard.response.AdminDashboardNewUsersResponse;
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

    @Operation(summary = "신규 가입자 추이 조회", description = "granularity=day면 일자별, week면 주별(ISO 8601, 월요일 시작) 신규 가입자 수를 반환한다.")
    @GetMapping("/new-users")
    public ApiResponse<AdminDashboardNewUsersResponse> getNewUsersTrend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "day") String granularity
    ) {
        return ApiResponse.onSuccess(adminDashboardService.getNewUsersTrend(from, to, granularity));
    }

    @Operation(summary = "배틀 참여율 조회", description = "기간(targetDate 기준) 내 발행된 배틀들의 배틀당 평균 사전투표/사후투표/관점작성/댓글작성 참여율을 반환한다.")
    @GetMapping("/battle-stats")
    public ApiResponse<AdminDashboardBattleStatsResponse> getBattleStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ApiResponse.onSuccess(adminDashboardService.getBattleStats(from, to));
    }

    @Operation(summary = "출석 체크율 조회", description = "기간 내 일자별 출석 인원, 총 출석 횟수, 평균 출석률, 개근 보너스(7일 연속 출석) 달성 건수를 반환한다.")
    @GetMapping("/attendance-stats")
    public ApiResponse<AdminDashboardAttendanceStatsResponse> getAttendanceStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ApiResponse.onSuccess(adminDashboardService.getAttendanceStats(from, to));
    }

    @Operation(summary = "크레딧 지급 현황 조회", description = "기간 내 크레딧 타입별 지급/차감 건수와 총액, 전체 지급/차감 합계를 반환한다.")
    @GetMapping("/credit-stats")
    public ApiResponse<AdminDashboardCreditStatsResponse> getCreditStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ApiResponse.onSuccess(adminDashboardService.getCreditStats(from, to));
    }
}

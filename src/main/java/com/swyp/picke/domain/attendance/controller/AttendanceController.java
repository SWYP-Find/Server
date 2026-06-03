package com.swyp.picke.domain.attendance.controller;

import com.swyp.picke.domain.attendance.dto.response.AttendanceResponse.CheckResponse;
import com.swyp.picke.domain.attendance.dto.response.AttendanceResponse.SummaryResponse;
import com.swyp.picke.domain.attendance.dto.response.AttendanceResponse.WeeklyResponse;
import com.swyp.picke.domain.attendance.service.AttendanceService;
import com.swyp.picke.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/attendance")
@Tag(name = "출석체크 API", description = "출석 체크, 주간 현황 조회, 통계 조회")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @Operation(summary = "출석 체크", description = "오늘의 출석을 체크하고 포인트를 지급합니다. 하루 1회만 가능합니다.")
    @PostMapping("/check")
    public ApiResponse<CheckResponse> check() {
        return ApiResponse.onSuccess(attendanceService.check());
    }

    @Operation(summary = "이번 주 출석 현황 조회", description = "이번 주(월~일) 요일별 출석 상태와 연속 출석 정보를 조회합니다.")
    @GetMapping("/weekly")
    public ApiResponse<WeeklyResponse> getWeekly() {
        return ApiResponse.onSuccess(attendanceService.getWeekly());
    }

    @Operation(summary = "출석 통계 조회", description = "총 출석 일수, 연속 출석 일수, 누적 포인트 등 전체 통계를 조회합니다.")
    @GetMapping("/summary")
    public ApiResponse<SummaryResponse> getSummary() {
        return ApiResponse.onSuccess(attendanceService.getSummary());
    }
}

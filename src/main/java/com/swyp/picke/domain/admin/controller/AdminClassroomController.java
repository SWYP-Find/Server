package com.swyp.picke.domain.admin.controller;

import com.swyp.picke.domain.admin.dto.classroom.response.AdminClassDetailResponse;
import com.swyp.picke.domain.admin.dto.classroom.response.AdminClassListItemResponse;
import com.swyp.picke.domain.admin.dto.classroom.response.AdminClassState;
import com.swyp.picke.domain.admin.dto.classroom.response.AdminClassSummaryResponse;
import com.swyp.picke.domain.admin.dto.classroom.response.AdminTicketSummaryResponse;
import com.swyp.picke.domain.admin.dto.classroom.request.AdminClassUpdateRequest;
import com.swyp.picke.domain.admin.dto.classroom.response.AdminClassAnalyticsResponse;
import com.swyp.picke.domain.admin.dto.classroom.response.AdminClassPerformanceResponse;
import com.swyp.picke.domain.admin.service.AdminClassUsageService;
import com.swyp.picke.domain.admin.service.AdminClassroomService;
import java.time.LocalDate;
import com.swyp.picke.domain.classroom.enums.ClassBattleType;
import com.swyp.picke.global.common.response.ApiResponse;
import com.swyp.picke.global.common.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 클래스", description = "클래스·참여 현황과 이용권 요약 조회")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
public class AdminClassroomController {

    private final AdminClassroomService adminClassroomService;
    private final AdminClassUsageService adminClassUsageService;

    @Operation(summary = "클래스 현황 요약")
    @GetMapping("/classes/summary")
    public ApiResponse<AdminClassSummaryResponse> getClassSummary() {
        return ApiResponse.onSuccess(adminClassroomService.getClassSummary());
    }

    @Operation(summary = "관리자 클래스 목록")
    @GetMapping("/classes")
    public ApiResponse<PageResponse<AdminClassListItemResponse>> searchClasses(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) AdminClassState state,
            @RequestParam(required = false) Integer capacity,
            @RequestParam(required = false) ClassBattleType battleType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.onSuccess(adminClassroomService.searchClasses(keyword, state, capacity, battleType, page, size));
    }

    @Operation(summary = "클래스 성과 분석", description = "이용권 발급·등록 현황(전체 누적)과 기간 안 사용 흐름·일별 리포트 완료·AI 작업 오류를 조회합니다. "
            + "from·to 는 한국 날짜(양끝 포함), 생략하면 오늘까지 최근 30일. 최대 366일.")
    @GetMapping("/classes/analytics")
    public ApiResponse<AdminClassAnalyticsResponse> getClassAnalytics(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        return ApiResponse.onSuccess(adminClassUsageService.analytics(from, to));
    }

    @Operation(summary = "클래스 성과", description = "배틀 시작 인원·개인 리포트 완료율·AI 작업 오류와 참가자별 댓글 수·리포트 상태를 조회합니다. 참가자는 페이지로 나눕니다.")
    @GetMapping("/classes/{classId}/performance")
    public ApiResponse<AdminClassPerformanceResponse> getClassPerformance(
            @PathVariable Long classId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.onSuccess(adminClassUsageService.classPerformance(classId, page, size));
    }

    @Operation(summary = "관리자 클래스 상세")
    @GetMapping("/classes/{classId}")
    public ApiResponse<AdminClassDetailResponse> getClassDetail(@PathVariable Long classId) {
        return ApiResponse.onSuccess(adminClassroomService.getClassDetail(classId));
    }

    @Operation(summary = "관리자 클래스 수정", description = "클래스 이름 변경과 마감일 연장만 허용합니다. 종료된 클래스는 수정할 수 없습니다.")
    @PatchMapping("/classes/{classId}")
    public ApiResponse<AdminClassDetailResponse> updateClass(
            @PathVariable Long classId,
            @Valid @RequestBody AdminClassUpdateRequest request) {
        return ApiResponse.onSuccess(adminClassroomService.updateClass(classId, request));
    }

    @Operation(summary = "클래스 인사이트 다시 만들기",
            description = "마감된 클래스의 운영자 인사이트를 현재 공개 글로 다시 만듭니다 (AI 1회). 의견 부족(LIMITED)·완료·실패 모두 가능, "
                    + "생성 대기·생성 중이면 409 CLASS_409_REPORT_RETRY. 1분 안에 스케줄러가 처리합니다.")
    @PostMapping("/classes/{classId}/insight/regenerate")
    public ApiResponse<com.swyp.picke.domain.admin.dto.classroom.response.AdminInsightRegenerateResponse> regenerateInsight(
            @PathVariable Long classId) {
        return ApiResponse.onSuccess(adminClassroomService.regenerateInsight(classId));
    }

    @Operation(summary = "이용권 현황 요약")
    @GetMapping("/tickets/summary")
    public ApiResponse<AdminTicketSummaryResponse> getTicketSummary() {
        return ApiResponse.onSuccess(adminClassroomService.getTicketSummary());
    }
}

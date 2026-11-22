package com.swyp.picke.domain.admin.controller;

import com.swyp.picke.domain.admin.dto.classroom.response.AdminClassDetailResponse;
import com.swyp.picke.domain.admin.dto.classroom.response.AdminClassListItemResponse;
import com.swyp.picke.domain.admin.dto.classroom.response.AdminClassState;
import com.swyp.picke.domain.admin.dto.classroom.response.AdminClassSummaryResponse;
import com.swyp.picke.domain.admin.dto.classroom.response.AdminTicketSummaryResponse;
import com.swyp.picke.domain.admin.dto.classroom.request.AdminClassUpdateRequest;
import com.swyp.picke.domain.admin.service.AdminClassroomService;
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

    @Operation(summary = "이용권 현황 요약")
    @GetMapping("/tickets/summary")
    public ApiResponse<AdminTicketSummaryResponse> getTicketSummary() {
        return ApiResponse.onSuccess(adminClassroomService.getTicketSummary());
    }
}

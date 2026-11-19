package com.swyp.picke.domain.admin.controller;

import com.swyp.picke.domain.admin.dto.classroom.AdminClassState;
import com.swyp.picke.domain.admin.dto.classroom.response.AdminClassDashboardSummaryResponse;
import com.swyp.picke.domain.admin.dto.classroom.response.AdminClassDetailResponse;
import com.swyp.picke.domain.admin.dto.classroom.response.AdminClassListItemResponse;
import com.swyp.picke.domain.admin.service.AdminClassroomService;
import com.swyp.picke.global.common.response.ApiResponse;
import com.swyp.picke.global.common.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.nio.charset.StandardCharsets;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 클래스 API", description = "클래스 관리 대시보드, 목록과 상세 조회")
@RestController
@RequestMapping("/api/v1/admin/classes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminClassroomController {
    private final AdminClassroomService adminClassroomService;

    @Operation(summary = "클래스 관리 요약 카드", description = "삭제되지 않은 클래스, 진행 중, 좌석 점유 인원, 30일 이내 마감 예정 수를 반환합니다.")
    @GetMapping("/summary")
    public ApiResponse<AdminClassDashboardSummaryResponse> getSummary() {
        return ApiResponse.onSuccess(adminClassroomService.getSummary());
    }

    @Operation(summary = "관리자 클래스 목록", description = "클래스명·생성자·연결 콘텐츠 검색과 OPEN/CLOSED 상태 필터를 지원합니다. 삭제된 클래스는 제외됩니다.")
    @GetMapping
    public ApiResponse<PageResponse<AdminClassListItemResponse>> getClasses(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) AdminClassState state,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.onSuccess(adminClassroomService.getClasses(keyword, state, page, size));
    }

    @Operation(summary = "관리자 클래스 목록 CSV 내보내기", description = "현재 검색어·상태 필터를 적용한 클래스와 참여 현황을 UTF-8 CSV로 내려줍니다.")
    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportClasses(@RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) AdminClassState state) {
        byte[] content = adminClassroomService.exportClasses(keyword, state);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("picke-classes.csv", StandardCharsets.UTF_8).build().toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .cacheControl(org.springframework.http.CacheControl.noStore())
                .body(content);
    }

    @Operation(summary = "관리자 클래스 상세", description = "클래스 설정, 연결 콘텐츠, 좌석 점유 인원, 활성 멤버를 반환합니다. 연락처와 계정 식별자는 포함하지 않습니다.")
    @GetMapping("/{classId}")
    public ApiResponse<AdminClassDetailResponse> getClassDetail(@PathVariable Long classId) {
        return ApiResponse.onSuccess(adminClassroomService.getClassDetail(classId));
    }
}

package com.swyp.picke.domain.classroom.controller;

import com.swyp.picke.domain.classroom.dto.response.ClassInsightResponse;
import com.swyp.picke.domain.classroom.dto.response.ClassMyReportResponse;
import com.swyp.picke.domain.classroom.dto.response.ClassResultResponse;
import com.swyp.picke.domain.classroom.service.ClassResultService;
import com.swyp.picke.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 클래스 결과·AI 리포트. 리포트는 마감(effective_close_at) 후 스케줄러가 1회 생성하며, API 는 저장된 결과를 읽기만 한다.
 * 공통 에러: 없는 클래스 404 CLASS_404, 멤버 아님 403 CLASS_403_MEMBER, 이용권 종료 + 30일 경과 409 CLASS_409_RESULT_EXPIRED.
 */
@Tag(name = "클래스 결과 API", description = "정량 통계·개인 AI 피드백·운영자 인사이트 (마감 후 생성, 이용권 종료 + 30일까지 열람)")
@RestController
@RequestMapping("/api/v1/classes/{classId}/result")
@RequiredArgsConstructor
public class ClassResultController {

    private final ClassResultService classResultService;

    @Operation(summary = "클래스 결과 (정량 통계)",
            description = "참여율·댓글 작성률·사전/사후 분포. 마감 전에는 현재 데이터(statsBasis=LIVE), 마감 후에는 마감 스냅샷(SNAPSHOT). "
                    + "myReportState / insightState 로 리포트 탭 노출을 결정합니다.")
    @GetMapping
    public ApiResponse<ClassResultResponse> getResult(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long classId
    ) {
        return ApiResponse.onSuccess(classResultService.getResult(userId, classId));
    }

    @Operation(summary = "내 AI 피드백",
            description = "참여 완료자에게 마감 후 제공. 마감 전 BEFORE_CLOSE(\"{closeAt} 이후 AI 피드백이 제공돼요\"), 참여 미완료 NOT_ELIGIBLE, "
                    + "글 없이 투표만 했으면 LIMITED(서버 템플릿). 참여하지 않는 운영자는 403 CLASS_403_MEMBER.")
    @GetMapping("/my-report")
    public ApiResponse<ClassMyReportResponse> getMyReport(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long classId
    ) {
        return ApiResponse.onSuccess(classResultService.getMyReport(userId, classId));
    }

    @Operation(summary = "클래스 인사이트 (운영자)",
            description = "공개 의견을 요약한 AI 인사이트와 근거·대표 의견. 운영자가 아니면 403 CLASS_403_OWNER. "
                    + "작성자 3명 미만이면 LIMITED, 생성 실패면 FAILED + retryable.")
    @GetMapping("/insight")
    public ApiResponse<ClassInsightResponse> getInsight(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long classId
    ) {
        return ApiResponse.onSuccess(classResultService.getInsight(userId, classId));
    }

    @Operation(summary = "클래스 인사이트 재시도 (운영자)",
            description = "FAILED 상태일 때만. 대기열에 다시 넣고 PENDING 을 돌려주며 1분 안에 재생성됩니다. 재시도 불가면 409 CLASS_409_REPORT_RETRY.")
    @PostMapping("/insight/retry")
    public ApiResponse<ClassInsightResponse> retryInsight(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long classId
    ) {
        return ApiResponse.onSuccess(classResultService.retryInsight(userId, classId));
    }
}

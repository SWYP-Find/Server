package com.swyp.picke.domain.perspective.controller;

import com.swyp.picke.domain.perspective.service.ReportService;
import com.swyp.picke.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "신고 (Report)", description = "관점/댓글 신고 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "관점 신고", description = "관점을 신고합니다. 신고 5회 누적 시 자동 숨김 처리됩니다.")
    @PostMapping("/perspectives/{perspectiveId}/reports")
    public ApiResponse<Void> reportPerspective(
            @PathVariable Long perspectiveId,
            @AuthenticationPrincipal Long userId) {
        reportService.reportPerspective(perspectiveId, userId);
        return ApiResponse.onSuccess(null);
    }

    @Operation(summary = "댓글 신고", description = "댓글을 신고합니다. 신고 5회 누적 시 자동 숨김 처리됩니다.")
    @PostMapping("/perspectives/{perspectiveId}/comments/{commentId}/reports")
    public ApiResponse<Void> reportComment(
            @PathVariable Long perspectiveId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal Long userId) {
        reportService.reportComment(commentId, userId);
        return ApiResponse.onSuccess(null);
    }
}

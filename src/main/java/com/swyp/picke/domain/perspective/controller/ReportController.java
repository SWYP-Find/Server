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

@Tag(name = "신고 API", description = "관점/댓글 신고")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "관점 신고", description = "관점을 신고합니다. 신고 10회 누적 시 자동 숨김 처리됩니다. 클래스 의견이면 운영자·참여 중인 멤버만 신고할 수 있습니다(403 CLASS_403_MEMBER).")
    @PostMapping("/perspectives/{perspectiveId}/reports")
    public ApiResponse<Void> reportPerspective(
            @PathVariable Long perspectiveId,
            @AuthenticationPrincipal Long userId) {
        reportService.reportPerspective(perspectiveId, userId);
        return ApiResponse.onSuccess(null);
    }

    @Operation(summary = "댓글 신고", description = "댓글을 신고합니다. 신고 10회 누적 시 자동 숨김 처리됩니다. 댓글이 경로의 관점에 속하지 않으면 404 COMMENT_NOT_FOUND, 클래스 댓글이면 운영자·참여 중인 멤버만 신고할 수 있습니다(403 CLASS_403_MEMBER).")
    @PostMapping("/perspectives/{perspectiveId}/comments/{commentId}/reports")
    public ApiResponse<Void> reportComment(
            @PathVariable Long perspectiveId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal Long userId) {
        reportService.reportComment(perspectiveId, commentId, userId);
        return ApiResponse.onSuccess(null);
    }
}

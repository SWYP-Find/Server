package com.swyp.app.domain.notice.controller;

import com.swyp.app.domain.notice.dto.response.NoticeDetailResponse;
import com.swyp.app.domain.notice.dto.response.NoticeListResponse;
import com.swyp.app.domain.notice.enums.NoticePlacement;
import com.swyp.app.domain.notice.enums.NoticeType;
import com.swyp.app.domain.notice.service.NoticeService;
import com.swyp.app.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "공지 API", description = "공지사항 조회")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notices")
public class NoticeController {

    private final NoticeService noticeService;

    @Operation(summary = "활성 공지 목록 조회")
    @GetMapping
    public ApiResponse<NoticeListResponse> getNotices(
            @RequestParam(required = false) NoticeType type,
            @RequestParam(required = false) NoticePlacement placement,
            @RequestParam(required = false) Integer limit
    ) {
        return ApiResponse.onSuccess(noticeService.getNoticeList(type, placement, limit));
    }

    @Operation(summary = "활성 공지 상세 조회")
    @GetMapping("/{noticeId}")
    public ApiResponse<NoticeDetailResponse> getNoticeDetail(@PathVariable Long noticeId) {
        return ApiResponse.onSuccess(noticeService.getNoticeDetail(noticeId));
    }
}

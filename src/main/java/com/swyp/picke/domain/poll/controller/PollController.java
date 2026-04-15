package com.swyp.picke.domain.poll.controller;

import com.swyp.picke.domain.poll.dto.response.PollDetailResponse;
import com.swyp.picke.domain.poll.dto.response.PollListResponse;
import com.swyp.picke.domain.poll.service.PollService;
import com.swyp.picke.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "투표 콘텐츠 API", description = "투표 콘텐츠 조회")
@RestController
@RequestMapping("/api/v1/polls")
@RequiredArgsConstructor
public class PollController {

    private final PollService pollService;

    @Operation(summary = "투표 콘텐츠 목록 조회")
    @GetMapping
    public ApiResponse<PollListResponse> getPolls(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        return ApiResponse.onSuccess(pollService.getPolls(page, size));
    }

    @Operation(summary = "투표 콘텐츠 상세 조회")
    @GetMapping("/{pollId}")
    public ApiResponse<PollDetailResponse> getPollDetail(@PathVariable Long pollId) {
        return ApiResponse.onSuccess(pollService.getPollDetail(pollId));
    }
}
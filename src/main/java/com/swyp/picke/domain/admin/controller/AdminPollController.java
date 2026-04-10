package com.swyp.picke.domain.admin.controller;

import com.swyp.picke.domain.admin.dto.poll.request.AdminPollCreateRequest;
import com.swyp.picke.domain.admin.dto.poll.request.AdminPollUpdateRequest;
import com.swyp.picke.domain.admin.dto.poll.response.AdminPollDeleteResponse;
import com.swyp.picke.domain.admin.dto.poll.response.AdminPollDetailResponse;
import com.swyp.picke.domain.admin.service.AdminPollService;
import com.swyp.picke.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 투표 콘텐츠 API", description = "관리자 투표 콘텐츠 생성, 조회, 수정, 삭제")
@RestController
@RequestMapping("/api/v1/admin/polls")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPollController {

    private final AdminPollService adminPollService;

    @Operation(summary = "투표 콘텐츠 생성")
    @PostMapping
    public ApiResponse<AdminPollDetailResponse> createPoll(@RequestBody @Valid AdminPollCreateRequest request) {
        return ApiResponse.onSuccess(adminPollService.createPoll(request));
    }

    @Operation(summary = "투표 콘텐츠 상세 조회")
    @GetMapping("/{pollId}")
    public ApiResponse<AdminPollDetailResponse> getPollDetail(@PathVariable Long pollId) {
        return ApiResponse.onSuccess(adminPollService.getPollDetail(pollId));
    }

    @Operation(summary = "투표 콘텐츠 수정")
    @PatchMapping("/{pollId}")
    public ApiResponse<AdminPollDetailResponse> updatePoll(
            @PathVariable Long pollId,
            @RequestBody @Valid AdminPollUpdateRequest request
    ) {
        return ApiResponse.onSuccess(adminPollService.updatePoll(pollId, request));
    }

    @Operation(summary = "투표 콘텐츠 삭제")
    @DeleteMapping("/{pollId}")
    public ApiResponse<AdminPollDeleteResponse> deletePoll(@PathVariable Long pollId) {
        return ApiResponse.onSuccess(adminPollService.deletePoll(pollId));
    }

    @Operation(summary = "투표 콘텐츠 목록 조회")
    @GetMapping
    public ApiResponse<?> getPolls(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "status", required = false) String status
    ) {
        return ApiResponse.onSuccess(adminPollService.getPolls(page, size, status));
    }
}
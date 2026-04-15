package com.swyp.picke.domain.admin.controller;

import com.swyp.picke.domain.admin.dto.quiz.request.AdminQuizCreateRequest;
import com.swyp.picke.domain.admin.dto.quiz.request.AdminQuizUpdateRequest;
import com.swyp.picke.domain.admin.dto.quiz.response.AdminQuizDeleteResponse;
import com.swyp.picke.domain.admin.dto.quiz.response.AdminQuizDetailResponse;
import com.swyp.picke.domain.admin.service.AdminQuizService;
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

@Tag(name = "관리자 퀴즈 API", description = "관리자 퀴즈 콘텐츠 생성, 조회, 수정, 삭제")
@RestController
@RequestMapping("/api/v1/admin/quizzes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminQuizController {

    private final AdminQuizService adminQuizService;

    @Operation(summary = "퀴즈 생성")
    @PostMapping
    public ApiResponse<AdminQuizDetailResponse> createQuiz(@RequestBody @Valid AdminQuizCreateRequest request) {
        return ApiResponse.onSuccess(adminQuizService.createQuiz(request));
    }

    @Operation(summary = "퀴즈 상세 조회")
    @GetMapping("/{quizId}")
    public ApiResponse<AdminQuizDetailResponse> getQuizDetail(@PathVariable Long quizId) {
        return ApiResponse.onSuccess(adminQuizService.getQuizDetail(quizId));
    }

    @Operation(summary = "퀴즈 수정")
    @PatchMapping("/{quizId}")
    public ApiResponse<AdminQuizDetailResponse> updateQuiz(
            @PathVariable Long quizId,
            @RequestBody @Valid AdminQuizUpdateRequest request
    ) {
        return ApiResponse.onSuccess(adminQuizService.updateQuiz(quizId, request));
    }

    @Operation(summary = "퀴즈 삭제")
    @DeleteMapping("/{quizId}")
    public ApiResponse<AdminQuizDeleteResponse> deleteQuiz(@PathVariable Long quizId) {
        return ApiResponse.onSuccess(adminQuizService.deleteQuiz(quizId));
    }

    @Operation(summary = "퀴즈 목록 조회")
    @GetMapping
    public ApiResponse<?> getQuizzes(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "status", required = false) String status
    ) {
        return ApiResponse.onSuccess(adminQuizService.getQuizzes(page, size, status));
    }
}
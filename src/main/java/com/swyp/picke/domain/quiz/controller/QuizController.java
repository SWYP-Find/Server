package com.swyp.picke.domain.quiz.controller;

import com.swyp.picke.domain.quiz.dto.response.QuizDetailResponse;
import com.swyp.picke.domain.quiz.dto.response.QuizListResponse;
import com.swyp.picke.domain.quiz.service.QuizService;
import com.swyp.picke.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "퀴즈 API", description = "퀴즈 콘텐츠 조회")
@RestController
@RequestMapping("/api/v1/quizzes")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @Operation(summary = "퀴즈 목록 조회")
    @GetMapping
    public ApiResponse<QuizListResponse> getQuizzes(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        return ApiResponse.onSuccess(quizService.getQuizzes(page, size));
    }

    @Operation(summary = "퀴즈 상세 조회")
    @GetMapping("/{quizId}")
    public ApiResponse<QuizDetailResponse> getQuizDetail(@PathVariable Long quizId) {
        return ApiResponse.onSuccess(quizService.getQuizDetail(quizId));
    }
}
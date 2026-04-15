package com.swyp.picke.domain.admin.dto.quiz.response;

import com.swyp.picke.domain.quiz.dto.response.QuizOptionResponse;
import com.swyp.picke.domain.quiz.enums.QuizStatus;

import java.time.LocalDate;
import java.util.List;

public record AdminQuizDetailResponse(
        Long quizId,
        String title,
        LocalDate targetDate,
        QuizStatus status,
        List<QuizOptionResponse> options
) {}
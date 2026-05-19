package com.swyp.picke.domain.admin.dto.quiz.response;

import com.swyp.picke.domain.quiz.dto.response.QuizOptionResponse;
import com.swyp.picke.domain.quiz.enums.QuizStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AdminQuizDetailResponse(
        Long quizId,
        String title,
        LocalDate targetDate,
        LocalDateTime publishAt,
        QuizStatus status,
        List<QuizOptionResponse> options
) {}

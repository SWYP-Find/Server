package com.swyp.picke.domain.quiz.dto.response;

import com.swyp.picke.domain.quiz.enums.QuizStatus;

import java.time.LocalDateTime;

public record QuizSimpleResponse(
        Long quizId,
        String title,
        QuizStatus status,
        LocalDateTime createdAt
) {
}



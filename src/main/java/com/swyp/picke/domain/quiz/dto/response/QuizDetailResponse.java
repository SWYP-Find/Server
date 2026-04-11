package com.swyp.picke.domain.quiz.dto.response;

import com.swyp.picke.domain.quiz.enums.QuizStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record QuizDetailResponse(
        Long quizId,
        String title,
        LocalDate targetDate,
        QuizStatus status,
        List<QuizOptionResponse> options,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

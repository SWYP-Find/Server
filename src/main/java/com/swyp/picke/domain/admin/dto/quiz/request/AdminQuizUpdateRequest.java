package com.swyp.picke.domain.admin.dto.quiz.request;

import com.swyp.picke.domain.quiz.enums.QuizStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AdminQuizUpdateRequest(
        String title,
        LocalDate targetDate,
        LocalDateTime publishAt,
        QuizStatus status,
        List<AdminQuizOptionRequest> options
) {
}



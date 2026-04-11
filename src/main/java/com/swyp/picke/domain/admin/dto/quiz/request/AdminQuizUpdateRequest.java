package com.swyp.picke.domain.admin.dto.quiz.request;

import com.swyp.picke.domain.quiz.enums.QuizStatus;

import java.time.LocalDate;
import java.util.List;

public record AdminQuizUpdateRequest(
        String title,
        LocalDate targetDate,
        QuizStatus status,
        List<AdminQuizOptionRequest> options
) {
}



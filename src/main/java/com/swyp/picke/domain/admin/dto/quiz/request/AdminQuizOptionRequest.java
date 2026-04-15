package com.swyp.picke.domain.admin.dto.quiz.request;

import com.swyp.picke.domain.quiz.enums.QuizOptionLabel;

public record AdminQuizOptionRequest(
        QuizOptionLabel label,
        String text,
        String detailText,
        Boolean isCorrect,
        Integer displayOrder
) {}

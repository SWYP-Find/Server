package com.swyp.picke.domain.quiz.dto.response;

import com.swyp.picke.domain.quiz.enums.QuizOptionLabel;

public record QuizOptionResponse(
        Long optionId,
        QuizOptionLabel label,
        String text,
        String detailText,
        Boolean isCorrect,
        Integer displayOrder
) {}
package com.swyp.picke.domain.quiz.dto.response;

import java.util.List;

public record QuizListResponse(
        List<QuizSimpleResponse> items,
        int page,
        int totalPages,
        long totalElements
) {
}



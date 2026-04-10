package com.swyp.picke.domain.admin.dto.quiz.response;

import java.time.LocalDateTime;

public record AdminQuizDeleteResponse(
        boolean success,
        LocalDateTime deletedAt
) {}
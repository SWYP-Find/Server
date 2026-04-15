package com.swyp.picke.domain.admin.dto.quiz.request;

import com.swyp.picke.domain.quiz.enums.QuizStatus;
import java.util.List;

public record AdminQuizCreateRequest(
        String title,
        QuizStatus status,
        List<AdminQuizOptionRequest> options
) {}
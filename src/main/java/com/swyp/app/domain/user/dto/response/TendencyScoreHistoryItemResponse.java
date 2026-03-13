package com.swyp.app.domain.user.dto.response;

import java.time.LocalDateTime;

public record TendencyScoreHistoryItemResponse(
        String historyId,
        int score1,
        int score2,
        int score3,
        int score4,
        int score5,
        int score6,
        LocalDateTime createdAt
) {
}

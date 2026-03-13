package com.swyp.app.domain.user.dto.request;

public record UpdateTendencyScoreRequest(
        int score1,
        int score2,
        int score3,
        int score4,
        int score5,
        int score6
) {
}

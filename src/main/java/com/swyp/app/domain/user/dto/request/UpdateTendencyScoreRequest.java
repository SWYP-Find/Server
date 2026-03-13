package com.swyp.app.domain.user.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateTendencyScoreRequest(
        @Min(-100) @Max(100)
        int score1,
        @Min(-100) @Max(100)
        int score2,
        @Min(-100) @Max(100)
        int score3,
        @Min(-100) @Max(100)
        int score4,
        @Min(-100) @Max(100)
        int score5,
        @Min(-100) @Max(100)
        int score6
) {
}

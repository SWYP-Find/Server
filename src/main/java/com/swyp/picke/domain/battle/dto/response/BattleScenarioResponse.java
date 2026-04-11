package com.swyp.picke.domain.battle.dto.response;

import java.util.List;

public record BattleScenarioResponse(
        String title,
        List<PhilosopherProfileResponse> philosophers
) {
    public record PhilosopherProfileResponse(
            String label,
            String name,
            String stance,
            String imageUrl
    ) {}
}

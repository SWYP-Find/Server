package com.swyp.app.domain.battle.dto.response;

import com.swyp.app.domain.battle.enums.BattleOptionLabel;

import java.util.UUID;

public record BattleOptionResponse(
        UUID optionId,
        BattleOptionLabel label,
        String title,
        String stance,
        String representative,
        String quote,
        String imageUrl
) {}

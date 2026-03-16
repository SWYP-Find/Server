package com.swyp.app.domain.battle.dto.request;

import com.swyp.app.domain.battle.enums.BattleOptionLabel;

import java.util.List;
import java.util.UUID;

public record AdminBattleOptionRequest(
        BattleOptionLabel label,
        String title,
        String stance,
        String representative,
        String quote,
        String imageUrl,
        List<UUID> philosopherTagIds,
        List<UUID> valueTagIds
) {}
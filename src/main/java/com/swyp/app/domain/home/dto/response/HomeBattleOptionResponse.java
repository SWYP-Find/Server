package com.swyp.app.domain.home.dto.response;

import com.swyp.app.domain.battle.enums.BattleOptionLabel;

public record HomeBattleOptionResponse(
        BattleOptionLabel label,
        String text
) {
}

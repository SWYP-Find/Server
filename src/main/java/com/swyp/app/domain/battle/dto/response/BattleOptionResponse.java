package com.swyp.app.domain.battle.dto.response;

import com.swyp.app.domain.battle.enums.BattleOptionLabel;

import java.util.List;

public record BattleOptionResponse(
        Long optionId,
        BattleOptionLabel label,
        String title,
        String stance,
        String representative,
        String quote,
        String imageUrl,
        List<BattleTagResponse> tags
) {}

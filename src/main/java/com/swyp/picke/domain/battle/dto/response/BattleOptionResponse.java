package com.swyp.picke.domain.battle.dto.response;

import com.swyp.picke.domain.battle.enums.BattleOptionLabel;

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

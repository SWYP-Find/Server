package com.swyp.picke.domain.battle.dto.response;

import com.swyp.picke.domain.battle.enums.BattleOptionLabel;

public record TodayOptionResponse(
        Long optionId,
        BattleOptionLabel label,
        String title,
        String representative,
        String stance,
        String imageUrl
) {}

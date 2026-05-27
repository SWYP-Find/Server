package com.swyp.picke.domain.battle.dto.response;

import java.util.List;

public record BattleOptionResponse(
        Long optionId,
        String title,
        String stance,
        String representative,
        String imageUrl,
        List<BattleTagResponse> tags
) {}

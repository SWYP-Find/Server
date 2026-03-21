package com.swyp.app.domain.home.dto.response;

import com.swyp.app.domain.battle.dto.response.BattleTagResponse;
import com.swyp.app.domain.battle.enums.BattleType;

import java.util.List;
import java.util.UUID;

public record HomeBattleResponse(
        UUID battleId,
        String title,
        String summary,
        String thumbnailUrl,
        BattleType type,
        Integer viewCount,
        Long participantsCount,
        Integer audioDuration,
        List<BattleTagResponse> tags,
        List<HomeBattleOptionResponse> options
) {
}

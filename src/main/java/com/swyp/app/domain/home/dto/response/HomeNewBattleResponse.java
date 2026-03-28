package com.swyp.app.domain.home.dto.response;

import com.swyp.app.domain.battle.dto.response.BattleTagResponse;

import java.util.List;

public record HomeNewBattleResponse(
        Long battleId,
        String thumbnailUrl,
        String title,
        String summary,
        String philosopherA,
        String philosopherAImageUrl,
        String philosopherB,
        String philosopherBImageUrl,
        List<BattleTagResponse> tags,
        Integer audioDuration,
        Integer viewCount
) {}

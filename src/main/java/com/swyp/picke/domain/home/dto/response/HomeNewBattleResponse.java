package com.swyp.picke.domain.home.dto.response;

import com.swyp.picke.domain.battle.dto.response.BattleTagResponse;

import java.util.List;

public record HomeNewBattleResponse(
        Long battleId,
        String thumbnailUrl,
        String title,
        String summary,
        String philosopherA,
        String optionATitle,
        String philosopherAImageUrl,
        String philosopherB,
        String optionBTitle,
        String philosopherBImageUrl,
        List<BattleTagResponse> tags,
        Integer audioDuration,
        Integer viewCount
) {}

package com.swyp.app.domain.home.dto.response;

import com.swyp.app.domain.battle.dto.response.BattleTagResponse;

import java.util.List;

public record HomeBestBattleResponse(
        Long battleId,
        String philosopherA,
        String philosopherB,
        String title,
        List<BattleTagResponse> tags,
        Integer audioDuration,
        Integer viewCount
) {}

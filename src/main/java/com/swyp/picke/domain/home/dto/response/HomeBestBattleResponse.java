package com.swyp.picke.domain.home.dto.response;

import com.swyp.picke.domain.battle.dto.response.BattleTagResponse;

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

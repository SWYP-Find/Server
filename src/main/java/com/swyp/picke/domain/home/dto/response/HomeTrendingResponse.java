package com.swyp.picke.domain.home.dto.response;

import com.swyp.picke.domain.battle.dto.response.BattleTagResponse;

import java.util.List;

public record HomeTrendingResponse(
        Long battleId,
        String thumbnailUrl,
        String title,
        List<BattleTagResponse> tags,
        Integer audioDuration,
        Integer viewCount
) {}

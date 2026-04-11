package com.swyp.picke.domain.battle.dto.response;

import java.util.List;

public record TodayBattleResponse(
        Long battleId,
        String title,
        String summary,
        String thumbnailUrl,
        Integer viewCount,
        Long participantsCount,
        Integer audioDuration,
        List<BattleTagResponse> tags,
        List<TodayOptionResponse> options
) {}

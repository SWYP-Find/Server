package com.swyp.picke.domain.search.dto.response;

import com.swyp.picke.domain.battle.dto.response.BattleTagResponse;

import java.util.List;

public record SearchBattleListResponse(
        List<SearchBattleItem> items,
        Integer nextOffset,
        boolean hasNext
) {

    public record SearchBattleItem(
            Long battleId,
            String thumbnailUrl,
            String title,
            String summary,
            List<BattleTagResponse> tags,
            Integer audioDuration,
            Integer viewCount
    ) {
    }
}

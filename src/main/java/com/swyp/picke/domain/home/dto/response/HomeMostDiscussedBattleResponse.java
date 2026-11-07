package com.swyp.picke.domain.home.dto.response;

import com.swyp.picke.domain.battle.dto.response.BattleTagResponse;
import java.util.List;

public record HomeMostDiscussedBattleResponse(
        String badgeLabel,
        Long battleId,
        String thumbnailUrl,
        String title,
        List<BattleTagResponse> tags,
        Integer audioDuration,
        Long commentCount,
        Integer viewCount
) {
}

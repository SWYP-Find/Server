package com.swyp.app.domain.home.dto.response;

import java.util.List;

public record HomeResponse(
        boolean newNotice,
        List<HomeBattleResponse> editorPicks,
        List<HomeBattleResponse> trendingBattles,
        List<HomeBattleResponse> bestBattles,
        List<HomeBattleResponse> todayPicks,
        List<HomeBattleResponse> newBattles
) {
}

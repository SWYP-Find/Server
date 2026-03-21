package com.swyp.app.domain.battle.service;

import com.swyp.app.domain.battle.dto.response.TodayBattleResponse;
import com.swyp.app.domain.battle.enums.BattleType;

import java.util.List;
import java.util.UUID;

public interface HomeServiceV2 {

    List<TodayBattleResponse> getEditorPicks();

    List<TodayBattleResponse> getTrendingBattles();

    List<TodayBattleResponse> getBestBattles();

    List<TodayBattleResponse> getTodayPicks(BattleType type);

    List<TodayBattleResponse> getNewBattles(List<UUID> excludeIds);
}

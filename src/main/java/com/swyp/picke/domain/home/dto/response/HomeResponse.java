package com.swyp.picke.domain.home.dto.response;

import java.util.List;

public record HomeResponse(
        boolean newNotice,
        List<HomeEditorPickResponse> editorPicks,
        List<HomeTrendingResponse> trendingBattles,
        List<HomeBestBattleResponse> bestBattles,
        List<HomeTodayQuizResponse> todayQuizzes,
        List<HomeTodayVoteResponse> todayVotes,
        List<HomeNewBattleResponse> newBattles
) {}

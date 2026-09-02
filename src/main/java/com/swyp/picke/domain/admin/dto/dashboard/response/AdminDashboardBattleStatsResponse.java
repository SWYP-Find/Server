package com.swyp.picke.domain.admin.dto.dashboard.response;

public record AdminDashboardBattleStatsResponse(
        long battleCount,
        double avgPreVoteRate,
        double avgPostVoteRate,
        double avgPerspectiveWriteRate,
        double avgCommentWriteRate
) {}

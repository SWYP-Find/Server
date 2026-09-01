package com.swyp.picke.domain.admin.dto.dashboard.response;

import java.util.List;

public record AdminDashboardAttendanceStatsResponse(
        double avgAttendanceRate,
        long totalCount,
        long streakAchievedCount,
        List<AdminDashboardTrendItemResponse> items
) {}

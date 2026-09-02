package com.swyp.picke.domain.admin.dto.dashboard.response;

public record AdminDashboardSummaryResponse(
        long newUserCount,
        long loginUserCount,
        long activeUserCount,
        long totalUserCount
) {}

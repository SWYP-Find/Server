package com.swyp.picke.domain.admin.dto.dashboard.response;

import java.util.List;

public record AdminDashboardNewUsersResponse(
        long totalCount,
        List<AdminDashboardTrendItemResponse> items
) {}

package com.swyp.picke.domain.admin.dto.dashboard.response;

import java.util.List;

public record AdminDashboardNewUsersResponse(
        List<AdminDashboardTrendItemResponse> items
) {}

package com.swyp.picke.domain.admin.dto.dashboard.response;

import java.util.List;

public record AdminDashboardCreditStatsResponse(
        long totalGranted,
        long totalDeducted,
        List<AdminDashboardCreditTypeStatResponse> byType
) {}

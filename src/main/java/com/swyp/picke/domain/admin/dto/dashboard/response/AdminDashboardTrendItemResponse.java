package com.swyp.picke.domain.admin.dto.dashboard.response;

import java.time.LocalDate;

public record AdminDashboardTrendItemResponse(
        LocalDate date,
        long count
) {}

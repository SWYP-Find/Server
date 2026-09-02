package com.swyp.picke.domain.admin.dto.dashboard.response;

import com.swyp.picke.domain.user.enums.CreditType;

public record AdminDashboardCreditTypeStatResponse(
        CreditType creditType,
        long count,
        long totalAmount
) {}

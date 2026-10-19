package com.swyp.picke.domain.admin.dto.funding.response;

import java.util.List;

public record FundingOrderPageResponse(
        List<FundingOrderSummaryResponse> items,
        int page,
        int size,
        long totalElements,
        boolean hasNext
) {}

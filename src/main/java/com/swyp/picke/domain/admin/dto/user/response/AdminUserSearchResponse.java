package com.swyp.picke.domain.admin.dto.user.response;

import java.util.List;

public record AdminUserSearchResponse(
        List<AdminUserSummaryResponse> items,
        boolean hasNext
) {}

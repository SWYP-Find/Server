package com.swyp.app.domain.user.dto.response;

import java.util.List;

public record TendencyScoreHistoryResponse(
        List<TendencyScoreHistoryItemResponse> items,
        Long nextCursor
) {
}

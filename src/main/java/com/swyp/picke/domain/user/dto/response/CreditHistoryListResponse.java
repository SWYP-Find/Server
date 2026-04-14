package com.swyp.picke.domain.user.dto.response;

import com.swyp.picke.domain.user.enums.CreditType;
import java.time.LocalDateTime;
import java.util.List;

public record CreditHistoryListResponse(
        List<CreditHistoryItem> items,
        Integer nextOffset,
        boolean hasNext
) {
    public record CreditHistoryItem(
            Long id,
            CreditType creditType,
            int amount,
            Long referenceId,
            LocalDateTime createdAt
    ) {}
}

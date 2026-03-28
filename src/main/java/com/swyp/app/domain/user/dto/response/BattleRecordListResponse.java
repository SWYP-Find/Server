package com.swyp.app.domain.user.dto.response;

import com.swyp.app.domain.user.entity.VoteSide;

import java.time.LocalDateTime;
import java.util.List;

public record BattleRecordListResponse(
        List<BattleRecordItem> items,
        Integer nextOffset,
        boolean hasNext
) {

    public record BattleRecordItem(
            String battleId,
            String recordId,
            VoteSide voteSide,
            String title,
            String summary,
            LocalDateTime createdAt
    ) {
    }
}

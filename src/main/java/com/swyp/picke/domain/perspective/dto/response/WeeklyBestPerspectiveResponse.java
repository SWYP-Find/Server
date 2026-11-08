package com.swyp.picke.domain.perspective.dto.response;

import java.time.LocalDate;
import java.util.List;

public record WeeklyBestPerspectiveResponse(
        LocalDate weekStart,
        LocalDate weekEnd,
        List<Item> items
) {
    public record Item(
            Long perspectiveId,
            Long battleId,
            String battleTitle,
            String content,
            long likeCount,
            boolean participated
    ) {}
}

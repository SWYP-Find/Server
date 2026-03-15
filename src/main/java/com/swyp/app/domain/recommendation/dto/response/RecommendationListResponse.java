package com.swyp.app.domain.recommendation.dto.response;

import java.util.List;
import java.util.UUID;

public record RecommendationListResponse(List<Item> items, String nextCursor, boolean hasNext) {

    public record Item(
            UUID battleId,
            String title,
            List<TagSummary> tags,
            int participantsCount,
            List<OptionSummary> options
    ) {}

    public record TagSummary(UUID tagId, String name) {}

    public record OptionSummary(
            UUID optionId,
            String label,
            String title,
            String representative,
            String imageUrl
    ) {}
}

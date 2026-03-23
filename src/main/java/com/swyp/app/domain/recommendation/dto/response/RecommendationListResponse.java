package com.swyp.app.domain.recommendation.dto.response;

import java.util.List;

public record RecommendationListResponse(List<Item> items, String nextCursor, boolean hasNext) {

    public record Item(
            Long battleId,
            String title,
            List<TagSummary> tags,
            int participantsCount,
            List<OptionSummary> options
    ) {}

    public record TagSummary(Long tagId, String name) {}

    public record OptionSummary(
            Long optionId,
            String label,
            String title,
            String representative,
            String imageUrl
    ) {}
}

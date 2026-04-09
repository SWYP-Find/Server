package com.swyp.picke.domain.recommendation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record RecommendationListResponse(List<Item> items, String nextCursor, boolean hasNext) {

    @Schema(name = "RecommendationItem")
    public record Item(
            Long battleId,
            String title,
            String summary,
            Integer audioDuration,
            Integer viewCount,
            List<TagSummary> tags,
            long participantsCount,
            List<OptionSummary> options
    ) {}

    @Schema(name = "RecommendationTagSummary")
    public record TagSummary(Long tagId, String name) {}

    @Schema(name = "RecommendationOptionSummary")
    public record OptionSummary(
            Long optionId,
            String label,
            String title,
            String stance,
            String representative,
            String imageUrl
    ) {}
}

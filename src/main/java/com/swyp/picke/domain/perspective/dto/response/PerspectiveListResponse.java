package com.swyp.picke.domain.perspective.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record PerspectiveListResponse(
        List<Item> items,
        String nextCursor,
        boolean hasNext
) {
    @Schema(name = "PerspectiveItem")
    public record Item(
            Long perspectiveId,
            UserSummary user,
            OptionSummary option,
            String content,
            int likeCount,
            int commentCount,
            boolean isLiked,
            boolean isMyPerspective,
            LocalDateTime createdAt
    ) {}

    @Schema(name = "PerspectiveUserSummary")
    public record UserSummary(
            String userTag,
            String nickname,
            String characterType,
            String characterImageUrl
    ) {}

    @Schema(name = "PerspectiveOptionSummary")
    public record OptionSummary(
            Long optionId,
            String label,
            String title,
            String stance
    ) {}
}

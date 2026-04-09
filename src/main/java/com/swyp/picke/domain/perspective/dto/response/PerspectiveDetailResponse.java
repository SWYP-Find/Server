package com.swyp.picke.domain.perspective.dto.response;

import java.time.LocalDateTime;

public record PerspectiveDetailResponse(
        Long perspectiveId,
        UserSummary user,
        OptionSummary option,
        String content,
        int likeCount,
        int commentCount,
        boolean isLiked,
        boolean isMyPerspective,
        LocalDateTime createdAt
) {
    public record UserSummary(String userTag, String nickname, String characterType, String characterImageUrl) {}

    public record OptionSummary(Long optionId, String label, String title, String stance) {}
}

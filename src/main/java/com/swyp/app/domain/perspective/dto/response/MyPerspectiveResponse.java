package com.swyp.app.domain.perspective.dto.response;

import com.swyp.app.domain.perspective.enums.PerspectiveStatus;

import java.time.LocalDateTime;

public record MyPerspectiveResponse(
        Long perspectiveId,
        UserSummary user,
        OptionSummary option,
        String content,
        int likeCount,
        int commentCount,
        boolean isLiked,
        PerspectiveStatus status,
        LocalDateTime createdAt
) {
    public record UserSummary(String userTag, String nickname, String characterType, String characterImageUrl) {}

    public record OptionSummary(Long optionId, String label, String title, String stance) {}
}

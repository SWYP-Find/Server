package com.swyp.app.domain.user.dto.response;

import com.swyp.app.domain.user.entity.ActivityType;
import com.swyp.app.domain.user.entity.CharacterType;

import java.time.LocalDateTime;
import java.util.List;

public record ContentActivityListResponse(
        List<ContentActivityItem> items,
        Integer nextOffset,
        boolean hasNext
) {

    public record ContentActivityItem(
            String activityId,
            ActivityType activityType,
            String perspectiveId,
            String battleId,
            String battleTitle,
            AuthorInfo author,
            String stance,
            String content,
            int likeCount,
            LocalDateTime createdAt
    ) {
    }

    public record AuthorInfo(
            String userTag,
            String nickname,
            CharacterType characterType
    ) {
    }
}

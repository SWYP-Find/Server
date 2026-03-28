package com.swyp.app.domain.user.dto.request;

public record UpdateNotificationSettingsRequest(
        Boolean newBattleEnabled,
        Boolean battleResultEnabled,
        Boolean commentReplyEnabled,
        Boolean newCommentEnabled,
        Boolean contentLikeEnabled,
        Boolean marketingEventEnabled
) {
}

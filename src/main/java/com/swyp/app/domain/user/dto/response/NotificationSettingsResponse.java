package com.swyp.app.domain.user.dto.response;

public record NotificationSettingsResponse(
        boolean newBattleEnabled,
        boolean battleResultEnabled,
        boolean commentReplyEnabled,
        boolean newCommentEnabled,
        boolean contentLikeEnabled,
        boolean marketingEventEnabled
) {
}

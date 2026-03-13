package com.swyp.app.domain.user.dto.request;

public record UpdateUserSettingsRequest(
        Boolean pushEnabled,
        Boolean emailEnabled,
        Boolean debateRequestEnabled,
        Boolean profilePublic
) {
}

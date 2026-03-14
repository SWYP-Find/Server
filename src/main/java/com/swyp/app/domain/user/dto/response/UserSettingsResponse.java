package com.swyp.app.domain.user.dto.response;

public record UserSettingsResponse(
        boolean pushEnabled,
        boolean emailEnabled,
        boolean debateRequestEnabled,
        boolean profilePublic
) {
}

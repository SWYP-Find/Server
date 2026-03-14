package com.swyp.app.domain.user.dto.request;

import jakarta.validation.constraints.AssertTrue;

public record UpdateUserSettingsRequest(
        Boolean pushEnabled,
        Boolean emailEnabled,
        Boolean debateRequestEnabled,
        Boolean profilePublic
) {
    @AssertTrue(message = "적어도 하나 이상의 설정값이 필요합니다.")
    public boolean hasAnySettingToUpdate() {
        return pushEnabled != null
                || emailEnabled != null
                || debateRequestEnabled != null
                || profilePublic != null;
    }
}

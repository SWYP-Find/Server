package com.swyp.app.domain.user.dto.response;

import com.swyp.app.domain.user.entity.CharacterType;
import com.swyp.app.domain.user.entity.UserStatus;

import java.math.BigDecimal;

public record OnboardingProfileResponse(
        String userTag,
        String nickname,
        CharacterType characterType,
        BigDecimal mannerTemperature,
        UserStatus status,
        boolean onboardingCompleted
) {
}

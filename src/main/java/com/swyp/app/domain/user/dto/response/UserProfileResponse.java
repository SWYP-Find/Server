package com.swyp.app.domain.user.dto.response;

import com.swyp.app.domain.user.entity.CharacterType;

import java.math.BigDecimal;

public record UserProfileResponse(
        String userTag,
        String nickname,
        CharacterType characterType,
        BigDecimal mannerTemperature
) {
}

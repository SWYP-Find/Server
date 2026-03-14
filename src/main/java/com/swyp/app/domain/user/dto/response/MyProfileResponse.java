package com.swyp.app.domain.user.dto.response;

import com.swyp.app.domain.user.entity.CharacterType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MyProfileResponse(
        String userTag,
        String nickname,
        CharacterType characterType,
        BigDecimal mannerTemperature,
        LocalDateTime updatedAt
) {
}

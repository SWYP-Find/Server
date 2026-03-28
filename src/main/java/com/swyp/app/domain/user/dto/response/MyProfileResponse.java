package com.swyp.app.domain.user.dto.response;

import com.swyp.app.domain.user.entity.CharacterType;

import java.time.LocalDateTime;

public record MyProfileResponse(
        String userTag,
        String nickname,
        CharacterType characterType,
        LocalDateTime updatedAt
) {
}

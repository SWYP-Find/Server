package com.swyp.app.domain.user.dto.request;

import com.swyp.app.domain.user.entity.CharacterType;

public record UpdateUserProfileRequest(
        String nickname,
        CharacterType characterType
) {
}

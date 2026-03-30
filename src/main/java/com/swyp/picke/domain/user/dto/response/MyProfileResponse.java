package com.swyp.picke.domain.user.dto.response;

import com.swyp.picke.domain.user.enums.CharacterType;

import java.time.LocalDateTime;

public record MyProfileResponse(
        String userTag,
        String nickname,
        CharacterType characterType,
        LocalDateTime updatedAt
) {
}

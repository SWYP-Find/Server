package com.swyp.app.domain.user.dto.request;

import com.swyp.app.domain.user.entity.CharacterType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
        @NotBlank
        @Size(min = 2, max = 20)
        String nickname,
        @NotNull
        CharacterType characterType
) {
}

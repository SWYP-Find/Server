package com.swyp.app.domain.user.dto.request;

import com.swyp.app.domain.user.entity.CharacterType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
        @Size(min = 2, max = 20)
        String nickname,
        CharacterType characterType
) {
    @AssertTrue(message = "적어도 하나 이상의 프로필 값이 필요합니다.")
    public boolean hasAnyFieldToUpdate() {
        return nickname != null || characterType != null;
    }

    @AssertTrue(message = "nickname은 공백만 입력할 수 없습니다.")
    public boolean hasValidNickname() {
        return nickname == null || !nickname.isBlank();
    }
}

package com.swyp.app.domain.user.dto.response;

import com.swyp.app.domain.user.entity.CharacterType;

public record UserSummary(String userTag, String nickname, CharacterType characterType) {}

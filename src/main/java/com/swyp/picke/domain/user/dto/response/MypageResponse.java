package com.swyp.picke.domain.user.dto.response;

import com.swyp.picke.domain.user.enums.CharacterType;
import com.swyp.picke.domain.user.enums.PhilosopherType;
import com.swyp.picke.domain.user.enums.TierCode;

import java.math.BigDecimal;

public record MypageResponse(
        ProfileInfo profile,
        PhilosopherInfo philosopher,
        TierInfo tier
) {

    public record ProfileInfo(
            String userTag,
            String nickname,
            CharacterType characterType,
            String characterLabel,
            String characterImageUrl,
            BigDecimal mannerTemperature
    ) {
    }

    public record PhilosopherInfo(
            PhilosopherType philosopherType,
            String philosopherLabel,
            String typeName,
            String description,
            String imageUrl
    ) {
    }

    public record TierInfo(
            TierCode tierCode,
            String tierLabel,
            int currentPoint
    ) {
    }
}

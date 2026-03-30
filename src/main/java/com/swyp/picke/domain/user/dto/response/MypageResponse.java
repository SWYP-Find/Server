package com.swyp.picke.domain.user.dto.response;

import com.swyp.picke.domain.user.entity.CharacterType;
import com.swyp.picke.domain.user.entity.PhilosopherType;
import com.swyp.picke.domain.user.entity.TierCode;

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

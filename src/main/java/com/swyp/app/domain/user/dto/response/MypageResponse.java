package com.swyp.app.domain.user.dto.response;

import com.swyp.app.domain.user.entity.CharacterType;
import com.swyp.app.domain.user.entity.PhilosopherType;
import com.swyp.app.domain.user.entity.TierCode;

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
            BigDecimal mannerTemperature
    ) {
    }

    public record PhilosopherInfo(
            PhilosopherType philosopherType
    ) {
    }

    public record TierInfo(
            TierCode tierCode,
            String tierLabel,
            int currentPoint
    ) {
    }
}

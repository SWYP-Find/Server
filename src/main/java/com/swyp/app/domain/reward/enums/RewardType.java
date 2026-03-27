package com.swyp.app.domain.reward.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RewardType {
    POINT, ITEM;

    public static RewardType from(String value) {
        for (RewardType type : RewardType.values()) {
            if (type.name().equalsIgnoreCase(value)) return type;
        }

        throw new IllegalArgumentException("REWARD_INVALID_TYPE");
    }
}

package com.swyp.app.domain.reward.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RewardItem {
    POINT, ITEM;

    public static RewardItem from(String value) {
        for (RewardItem type : RewardItem.values()) {
            if (type.name().equalsIgnoreCase(value)) return type;
        }

        throw new IllegalArgumentException("REWARD_INVALID_TYPE");
    }
}

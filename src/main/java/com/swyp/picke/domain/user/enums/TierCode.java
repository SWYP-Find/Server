package com.swyp.picke.domain.user.enums;

import lombok.Getter;

@Getter
public enum TierCode {
    WANDERER("방랑자", 0),
    STUDENT("학도", 500),
    SAGE("현자", 2000),
    PHILOSOPHER("철학자", 5000),
    MASTER("마스터", 10000);

    private final String label;
    private final int minPoints;

    TierCode(String label, int minPoints) {
        this.label = label;
        this.minPoints = minPoints;
    }

    public static TierCode fromPoints(int points) {
        TierCode result = WANDERER;
        for (TierCode tier : values()) {
            if (points >= tier.minPoints) {
                result = tier;
            }
        }
        return result;
    }
}

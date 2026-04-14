package com.swyp.picke.domain.user.enums;

import lombok.Getter;

@Getter
public enum CreditType {
    BATTLE_VOTE(10),
    QUIZ_VOTE(5),
    MAJORITY_WIN(20),
    BEST_COMMENT(50),
    TOPIC_SUGGEST(30),
    TOPIC_ADOPTED(1000),
    AD_REWARD(50),
    FREE_CHARGE(0);

    private final int defaultAmount;

    CreditType(int defaultAmount) {
        this.defaultAmount = defaultAmount;
    }
}

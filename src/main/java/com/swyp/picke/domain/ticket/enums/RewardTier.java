package com.swyp.picke.domain.ticket.enums;

import lombok.Getter;

/**
 * 펀딩 리워드 티어. 이용 기간은 6개월 고정이고 좌석 수만 다르다.
 */
@Getter
public enum RewardTier {
    SEAT_5(5, 9_900),
    SEAT_15(15, 19_900),
    SEAT_30(30, 29_900);

    private final int capacity;
    private final int price;

    RewardTier(int capacity, int price) {
        this.capacity = capacity;
        this.price = price;
    }
}

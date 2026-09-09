package com.swyp.picke.domain.ad.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 제휴 광고 매체. AdMob 리워드 광고(reward 도메인)와는 무관하다.
 */
@Getter
@RequiredArgsConstructor
public enum AdNetwork {

    COUPANG("쿠팡 파트너스"),
    ADPICK("애드픽");

    private final String description;
}

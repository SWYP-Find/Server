package com.swyp.picke.domain.funding.service;

import com.swyp.picke.domain.funding.enums.TumblbugPlan;

/** 요금제별 텀블벅 전달 기능을 구분하는 전략 계약. */
public interface TumblbugPlanHandler {
    TumblbugPlan plan();

    boolean supportsDigitalGiftWorkbook();
}

package com.swyp.picke.domain.funding.service;

import com.swyp.picke.domain.funding.enums.TumblbugPlan;
import org.springframework.stereotype.Component;

/** Start: 명단 업로드·수동 등록·코드 발급 후 텀블벅 1:1 메시지로 전달합니다. */
@Component
public class StartTumblbugPlanHandler implements TumblbugPlanHandler {
    @Override
    public TumblbugPlan plan() {
        return TumblbugPlan.START;
    }

    @Override
    public boolean supportsDigitalGiftWorkbook() {
        return false;
    }
}

package com.swyp.picke.domain.funding.service;

import com.swyp.picke.domain.funding.enums.TumblbugPlan;
import org.springframework.stereotype.Component;

/** Run: Start 공통 기능에 텀블벅 디지털 선물 코드 XLSX 전달을 더합니다. */
@Component
public class RunTumblbugPlanHandler implements TumblbugPlanHandler {
    @Override
    public TumblbugPlan plan() {
        return TumblbugPlan.RUN;
    }

    @Override
    public boolean supportsDigitalGiftWorkbook() {
        return true;
    }
}

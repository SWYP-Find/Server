package com.swyp.picke.domain.funding.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "캠페인 주문 접수 상태")
public enum FundingCampaignStatus {
    ACTIVE("주문 등록 가능"),
    PAUSED("일시 중지"),
    CLOSED("종료");

    private final String description;

    FundingCampaignStatus(String description) {
        this.description = description;
    }
}

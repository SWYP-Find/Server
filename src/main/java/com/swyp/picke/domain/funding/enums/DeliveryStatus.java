package com.swyp.picke.domain.funding.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "등록 코드 전달 결과")
public enum DeliveryStatus {
    PENDING("전달 결과 미기록"),
    SENT("전달 완료"),
    FAILED("전달 실패");

    private final String description;

    DeliveryStatus(String description) {
        this.description = description;
    }
}

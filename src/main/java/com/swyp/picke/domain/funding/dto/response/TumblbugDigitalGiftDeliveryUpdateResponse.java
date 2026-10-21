package com.swyp.picke.domain.funding.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "텀블벅 디지털 선물 업로드 결과 반영 건수")
public record TumblbugDigitalGiftDeliveryUpdateResponse(
        int requestedSupporterCount,
        int matchedOrderCount,
        int updatedAttemptCount,
        int supportersWithoutOrderCount
) {
}

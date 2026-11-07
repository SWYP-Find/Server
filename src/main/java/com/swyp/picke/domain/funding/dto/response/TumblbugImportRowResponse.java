package com.swyp.picke.domain.funding.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "사진의 후원자 관리 표와 대응하는 엑셀 행 검토 결과")
public record TumblbugImportRowResponse(
        int rowNumber,
        String supporterNumber,
        String buyerName,
        String giftName,
        Long rewardAmount,
        Long additionalSupportAmount,
        Long totalSupportAmount,
        LocalDateTime supportedAt,
        String notificationRequest,
        String paymentStatus,
        String giftDeliveryStatus,
        Integer quantity,
        String result,
        String message
) {
}

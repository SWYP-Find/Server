package com.swyp.picke.domain.admin.dto.funding.response;

import com.swyp.picke.domain.funding.enums.DeliveryStatus;
import com.swyp.picke.domain.funding.enums.TumblbugPlan;
import com.swyp.picke.domain.ticket.enums.PaymentStatus;
import java.time.LocalDateTime;
import java.util.List;

/** 수동 등록 결과. 발급 코드는 관리자 응답에만 포함하고 로그에는 기록하지 않습니다. */
public record ManualTumblbugBuyerResponse(
        Long fundingOrderId,
        TumblbugPlan plan,
        String campaignId,
        String supporterNumber,
        String buyerName,
        String rewardName,
        int quantity,
        LocalDateTime supportedAt,
        long rewardAmount,
        long additionalSupportAmount,
        long totalSupportAmount,
        PaymentStatus paymentStatus,
        DeliveryStatus giftDeliveryStatus,
        List<IssuedCode> issuedCodes
) {
    public record IssuedCode(Long ticketId, String code) {}
}

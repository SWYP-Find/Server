package com.swyp.picke.domain.funding.dto.response;

import com.swyp.picke.domain.funding.enums.DeliveryStatus;
import com.swyp.picke.domain.funding.enums.TumblbugPlan;
import com.swyp.picke.domain.ticket.enums.PaymentStatus;
import java.time.LocalDateTime;
import java.util.List;

/** Start·Run 공통 업로드 결과. 전체 코드는 관리자 전용 응답에서만 전달합니다. */
public record TumblbugSupporterImportResponse(
        TumblbugPlan plan,
        int importedCount,
        int issuedCodeCount,
        List<Buyer> buyers
) {
    public record Buyer(
            int rowNumber,
            String supporterNumber,
            String buyerName,
            String rewardName,
            int quantity,
            LocalDateTime supportedAt,
            Long rewardAmount,
            Long additionalSupportAmount,
            Long totalSupportAmount,
            PaymentStatus paymentStatus,
            DeliveryStatus giftDeliveryStatus,
            List<IssuedCode> issuedCodes
    ) {}

    public record IssuedCode(Long ticketId, String code) {}
}

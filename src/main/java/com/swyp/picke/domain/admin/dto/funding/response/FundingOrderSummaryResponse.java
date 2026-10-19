package com.swyp.picke.domain.admin.dto.funding.response;

import com.swyp.picke.domain.ticket.enums.FundingPlatform;
import com.swyp.picke.domain.ticket.enums.PaymentStatus;
import com.swyp.picke.domain.ticket.enums.RewardTier;
import com.swyp.picke.domain.ticket.enums.TicketCodeStatus;
import com.swyp.picke.domain.ticket.enums.TicketStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** 주문 + 살아 있는 이용권 요약. 코드 원문·해시는 절대 포함하지 않는다. */
public record FundingOrderSummaryResponse(
        Long fundingOrderId,
        FundingPlatform platform,
        String campaignId,
        String orderId,
        RewardTier rewardTier,
        int quantity,
        String supporterName,
        @Schema(nullable = true) String supporterEmail,
        @Schema(nullable = true) String supporterPhone,
        PaymentStatus paymentStatus,
        @Schema(nullable = true) LocalDateTime paidAt,
        @Schema(description = "발급 가능 여부 (결제 확정·수량 1·결제일 있음·미발급)") boolean issuable,
        @Schema(description = "살아 있는(REVOKED 아닌) 이용권. 없으면 null", nullable = true) Ticket ticket
) {
    public record Ticket(
            Long ticketId,
            int capacity,
            TicketCodeStatus codeStatus,
            TicketStatus status,
            @Schema(nullable = true) LocalDateTime deliveredAt,
            LocalDateTime registerBy,
            @Schema(nullable = true) LocalDateTime activatedAt,
            @Schema(nullable = true) LocalDateTime expiresAt,
            @Schema(nullable = true) Long registeredUserId,
            @Schema(nullable = true) String registeredNickname
    ) {}
}

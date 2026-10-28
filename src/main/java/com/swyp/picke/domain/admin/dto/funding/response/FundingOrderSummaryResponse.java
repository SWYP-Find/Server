package com.swyp.picke.domain.admin.dto.funding.response;

import com.swyp.picke.domain.ticket.enums.FundingPlatform;
import com.swyp.picke.domain.ticket.enums.PaymentStatus;
import com.swyp.picke.domain.ticket.enums.RewardTier;
import com.swyp.picke.domain.ticket.enums.TicketCodeStatus;
import com.swyp.picke.domain.ticket.enums.TicketStatus;
import com.swyp.picke.domain.funding.dto.response.TumblbugOrderDetailResponse;
import com.swyp.picke.domain.funding.enums.DeliveryStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/** 주문 + 살아 있는 이용권 요약. 코드 원문은 별도 관리자 조회 API로만 반환합니다. */
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
        @Schema(description = "결제 완료·결제일 존재·미발급 수량이 남아 있는지") boolean issuable,
        @Schema(description = "살아 있는(REVOKED 아닌) 이용권. 없으면 null", nullable = true) Ticket ticket,
        @Schema(description = "주문 수량만큼 발급된 살아 있는 이용권 목록") List<Ticket> tickets,
        @Schema(description = "텀블벅 XLSX 원본 정보. CSV 주문은 null", nullable = true) TumblbugOrderDetailResponse tumblbug,
        @Schema(description = "최근 전달 결과. 이력이 없으면 PENDING")
        DeliveryStatus deliveryStatus
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
            @Schema(nullable = true) String registeredNickname,
            @Schema(nullable = true, description = "관리자 조회용 화면에서 코드를 찾을 때 보조하는 끝 4자리") String codeSuffix,
            DeliveryStatus deliveryStatus
    ) {}
}

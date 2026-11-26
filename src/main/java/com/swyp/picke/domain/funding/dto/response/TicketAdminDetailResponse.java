package com.swyp.picke.domain.funding.dto.response;

import com.swyp.picke.domain.admin.dto.classroom.response.AdminClassState;
import com.swyp.picke.domain.ticket.enums.DeliveryChannel;
import com.swyp.picke.domain.ticket.enums.DeliveryStatus;
import com.swyp.picke.domain.ticket.enums.FundingPlatform;
import com.swyp.picke.domain.ticket.enums.PaymentStatus;
import com.swyp.picke.domain.ticket.enums.RewardTier;
import com.swyp.picke.domain.ticket.enums.TicketCodeStatus;
import com.swyp.picke.domain.ticket.enums.TicketStatus;
import java.time.LocalDateTime;
import java.util.List;

/** 관리자 이용권 상세. 코드 원문은 감사 이력을 남기는 별도 API로 조회한다. */
public record TicketAdminDetailResponse(
        Long ticketId,
        Long fundingOrderId,
        String maskedCode,
        boolean codeAvailable,
        String buyerName,
        String buyerEmail,
        String buyerPhone,
        Long registeredUserId,
        String registeredNickname,
        FundingPlatform platform,
        String campaignId,
        String orderId,
        RewardTier rewardTier,
        int purchaseQuantity,
        int capacity,
        PaymentStatus paymentStatus,
        TicketStatus status,
        TicketCodeStatus codeStatus,
        LocalDateTime issuedAt,
        LocalDateTime deliveredAt,
        LocalDateTime registerBy,
        LocalDateTime activatedAt,
        LocalDateTime expiresAt,
        LocalDateTime revokedAt,
        List<Delivery> deliveries,
        List<ClassItem> classes,
        // 등록 계정 사용자 태그 · 이메일 (소셜 계정). 미등록이면 null
        String registeredUserTag,
        String registeredEmail,
        // 등록 후 이용 기간(일). null 이면 기본 6개월
        Integer usageDays
) {
    public record Delivery(DeliveryChannel channel, DeliveryStatus status, String failureReason,
                           LocalDateTime attemptedAt) {}

    public record ClassItem(Long classId, String name, int participantCount, int capacity,
                            AdminClassState state, LocalDateTime createdAt) {}
}

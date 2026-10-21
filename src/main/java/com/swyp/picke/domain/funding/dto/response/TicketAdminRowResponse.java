package com.swyp.picke.domain.funding.dto.response;

import com.swyp.picke.domain.ticket.enums.TicketStatus;
import com.swyp.picke.domain.ticket.enums.TicketCodeStatus;
import java.time.LocalDateTime;

public record TicketAdminRowResponse(
        // 이용권 ID
        Long ticketId,
        // 마스킹된 등록 코드
        String maskedCode,
        // 구매자 이름
        String buyerName,
        // 마스킹된 구매자 연락처
        String maskedBuyerContact,
        // 이용권 소유자 닉네임
        String ownerNickname,
        // 이용권 정원
        int capacity,
        // 이용권 상태
        TicketStatus status,
        // 등록 코드 상태
        TicketCodeStatus ticketCodeStatus,
        // 코드 등록 마감 시각
        LocalDateTime registerBy,
        // 이용권 활성화 시각
        LocalDateTime activatedAt,
        // 이용권 만료 시각
        LocalDateTime expiresAt
) {
}

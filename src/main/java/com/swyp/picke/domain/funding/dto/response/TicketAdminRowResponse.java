package com.swyp.picke.domain.funding.dto.response;

import com.swyp.picke.domain.ticket.enums.TicketStatus;
import com.swyp.picke.domain.ticket.enums.TicketCodeStatus;
import java.time.LocalDateTime;

public record TicketAdminRowResponse(
        // 이용권 ID
        Long ticketId,
        // 연결된 펀딩 주문 ID
        Long fundingOrderId,
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
        LocalDateTime expiresAt,
        // 코드 원문을 확인할 수 있는지 (암호화 원문 보관 여부)
        boolean codeAvailable,
        // 등록 계정 ID. 미등록이면 null
        Long registeredUserId,
        // 마스킹된 등록 계정 이메일 (소셜 계정). 미등록이거나 이메일이 없으면 null
        String maskedAccountEmail,
        // 이 이용권으로 만든 클래스 수 (삭제 제외)
        long classCount,
        // 연결된 클래스의 마지막 활동 시각. 없으면 null
        LocalDateTime lastActiveAt
) {
}

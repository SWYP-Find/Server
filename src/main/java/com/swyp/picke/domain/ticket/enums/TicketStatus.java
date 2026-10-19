package com.swyp.picke.domain.ticket.enums;

/** 이용권 자체의 생명주기 */
public enum TicketStatus {
    UNREGISTERED,      // 발급됐지만 아직 등록 전
    ACTIVE,            // 등록 완료, 이용 기간 중
    EXPIRED,           // 이용 기간(6개월) 종료
    REGISTER_EXPIRED,  // 등록기한(결제 확정일 + 1년) 경과
    REVOKED            // 무효화
}

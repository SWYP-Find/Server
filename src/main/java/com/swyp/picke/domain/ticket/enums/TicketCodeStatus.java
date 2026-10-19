package com.swyp.picke.domain.ticket.enums;

/** 이용권 코드의 전달·사용 상태 */
public enum TicketCodeStatus {
    ISSUED,     // 코드 생성됨, 아직 전달 전
    DELIVERED,  // 구매자에게 전달 완료
    CLAIMED,    // 사용자가 등록함
    REVOKED     // 재발급 등으로 무효화
}

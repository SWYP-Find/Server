package com.swyp.picke.domain.classroom;

/** Classroom은 클래스 생성 전 이 계약만 호출하고, Funding 데이터 변경은 수행하지 않습니다. */
public interface FundingTicketPolicy {
    FundingTicketAccess requireUsableBy(Long userId, Long ticketId);
}

package com.swyp.picke.domain.classroom;

import java.time.LocalDateTime;

// Funding 도메인이 클래스 생성 시 제공하는 최소 이용권 정보입니다.
public record FundingTicketAccess(Long ticketId, int capacity, LocalDateTime expiresAt) {
}

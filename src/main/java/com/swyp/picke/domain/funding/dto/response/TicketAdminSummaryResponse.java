package com.swyp.picke.domain.funding.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** 관리자 이용권 목록 화면의 요약 카드. */
public record TicketAdminSummaryResponse(
        @Schema(description = "무효화되지 않은 전체 이용권 수") long totalTickets,
        @Schema(description = "현재 이용 중인 이용권 수") long inUse,
        @Schema(description = "아직 등록되지 않은 이용권 수. 등록기한이 지난 코드도 포함") long unregistered,
        @Schema(description = "이용권 종료일이 30일 이내로 남은 이용 중인 이용권 수") long expiringWithin30Days
) {
}

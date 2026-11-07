package com.swyp.picke.domain.admin.dto.funding.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** 어드민 펀딩 대시보드 카운트. 이용권 수치는 살아 있는(REVOKED 아닌) 행 기준. */
public record FundingSummaryResponse(
        @Schema(description = "전체 주문 수") long totalOrders,
        @Schema(description = "결제 확정 주문 수") long paidConfirmed,
        @Schema(description = "발급 가능 (결제 완료·남은 수량 있음)") long issuable,
        @Schema(description = "보류 (결제 확정이지만 수량이 1이 아니거나 결제일 없음, 결제 대기·예약)") long held,
        @Schema(description = "발급된 이용권 수 (살아 있는 행)") long issued,
        @Schema(description = "미전달 (codeStatus=ISSUED)") long undelivered,
        @Schema(description = "전달 완료 (codeStatus=DELIVERED, 아직 미등록)") long delivered,
        @Schema(description = "등록 완료 (ACTIVE + EXPIRED)") long registered,
        @Schema(description = "환불 주문 수") long refunded,
        @Schema(description = "취소 주문 수") long canceled,
        @Schema(description = "등록기한 30일 전 알림 대상 수") long registerReminderD30,
        @Schema(description = "등록기한 7일 전 알림 대상 수") long registerReminderD7
) {}

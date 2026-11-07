package com.swyp.picke.domain.admin.dto.funding.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record FundingRefundResponse(
        @Schema(description = "이번 요청으로 REFUNDED 가 된 주문 수") int refunded,
        @Schema(description = "함께 무효화(REVOKED)된 이용권 수") int revokedTickets,
        @Schema(description = "이미 REFUNDED 였던 주문 ID") List<Long> alreadyRefunded
) {}

package com.swyp.picke.domain.admin.dto.funding.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record TicketIssueRequest(
        @Schema(description = "발급할 펀딩 주문 ID 목록. 비우면 발급 가능한 전체 주문(결제 확정·수량 1·미발급)을 대상으로 한다", nullable = true)
        List<Long> fundingOrderIds
) {}

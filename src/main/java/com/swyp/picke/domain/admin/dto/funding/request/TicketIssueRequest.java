package com.swyp.picke.domain.admin.dto.funding.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record TicketIssueRequest(
        @Schema(description = "발급할 펀딩 주문 ID 목록. 비우면 결제 완료된 전체 주문의 미발급 수량을 대상으로 한다", nullable = true)
        List<Long> fundingOrderIds
) {}

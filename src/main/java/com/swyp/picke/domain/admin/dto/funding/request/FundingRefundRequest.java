package com.swyp.picke.domain.admin.dto.funding.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record FundingRefundRequest(
        @Schema(description = "환불 처리할 펀딩 주문 ID 목록")
        @NotEmpty List<Long> fundingOrderIds
) {}

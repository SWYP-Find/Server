package com.swyp.picke.domain.admin.dto.funding.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record TicketIssueRequest(
        @NotEmpty @Schema(description = "선택한 발급 대상 펀딩 주문 ID 목록")
        List<@NotNull Long> fundingOrderIds
) {}

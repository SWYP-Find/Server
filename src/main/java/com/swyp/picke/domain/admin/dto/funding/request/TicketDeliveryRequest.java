package com.swyp.picke.domain.admin.dto.funding.request;

import com.swyp.picke.domain.ticket.enums.DeliveryChannel;
import com.swyp.picke.domain.ticket.enums.DeliveryStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TicketDeliveryRequest(
        @Schema(description = "전달 수단", example = "EMAIL")
        @NotNull DeliveryChannel channel,
        @Schema(description = "전달 결과. SUCCESS 면 이용권 코드 상태가 DELIVERED 로 바뀐다", example = "SUCCESS")
        @NotNull DeliveryStatus status,
        @Schema(description = "실패 사유 (FAILED 일 때)", nullable = true)
        @Size(max = 255) String failureReason
) {}

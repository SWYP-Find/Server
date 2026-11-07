package com.swyp.picke.domain.admin.dto.funding.response;

import com.swyp.picke.domain.ticket.enums.DeliveryChannel;
import com.swyp.picke.domain.ticket.enums.DeliveryStatus;
import com.swyp.picke.domain.ticket.enums.TicketCodeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record TicketDeliveryResponse(
        Long attemptId,
        Long ticketId,
        DeliveryChannel channel,
        DeliveryStatus status,
        @Schema(nullable = true) String failureReason,
        LocalDateTime attemptedAt,
        @Schema(description = "기록 후 이용권 코드 상태") TicketCodeStatus codeStatus,
        @Schema(nullable = true) LocalDateTime deliveredAt
) {}

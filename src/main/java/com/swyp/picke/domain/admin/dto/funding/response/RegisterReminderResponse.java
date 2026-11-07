package com.swyp.picke.domain.admin.dto.funding.response;

import com.swyp.picke.domain.ticket.enums.DeliveryChannel;
import com.swyp.picke.domain.ticket.enums.DeliveryStatus;
import com.swyp.picke.domain.ticket.enums.RegisterReminderStage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record RegisterReminderResponse(
        Long reminderId,
        Long ticketId,
        RegisterReminderStage stage,
        DeliveryChannel channel,
        DeliveryStatus status,
        @Schema(nullable = true) String failureReason,
        LocalDateTime attemptedAt
) {}

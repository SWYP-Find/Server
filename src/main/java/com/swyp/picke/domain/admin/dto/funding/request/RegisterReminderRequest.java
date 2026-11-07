package com.swyp.picke.domain.admin.dto.funding.request;

import com.swyp.picke.domain.ticket.enums.DeliveryChannel;
import com.swyp.picke.domain.ticket.enums.DeliveryStatus;
import com.swyp.picke.domain.ticket.enums.RegisterReminderStage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterReminderRequest(
        @Schema(description = "알림 단계 (D30: 등록기한 30일 전, D7: 7일 전)", example = "D30")
        @NotNull RegisterReminderStage stage,
        @Schema(description = "발송 수단", example = "EMAIL")
        @NotNull DeliveryChannel channel,
        @Schema(description = "발송 결과. SUCCESS 로 기록한 단계는 알림 대상에서 빠진다", example = "SUCCESS")
        @NotNull DeliveryStatus status,
        @Schema(description = "실패 사유 (FAILED 일 때)", nullable = true)
        @Size(max = 255) String failureReason
) {}

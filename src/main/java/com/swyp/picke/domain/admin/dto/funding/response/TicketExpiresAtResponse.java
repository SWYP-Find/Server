package com.swyp.picke.domain.admin.dto.funding.response;

import com.swyp.picke.domain.ticket.enums.TicketStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "만료일 수정 결과")
public record TicketExpiresAtResponse(
        Long ticketId,
        @Schema(description = "등록(이용 시작) 시각") LocalDateTime activatedAt,
        @Schema(description = "바뀌기 전 만료 시각") LocalDateTime previousExpiresAt,
        @Schema(description = "바뀐 만료 시각") LocalDateTime expiresAt,
        @Schema(description = "결과 조회 마감 (만료 + 30일)") LocalDateTime readGraceEndsAt,
        @Schema(description = "이용권 상태 (만료됐던 이용권을 늘리면 ACTIVE)") TicketStatus status
) {
}

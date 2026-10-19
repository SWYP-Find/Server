package com.swyp.picke.domain.ticket.dto.response;

import com.swyp.picke.domain.ticket.enums.TicketStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record ClassTicketResponse(
        Long ticketId,
        @Schema(description = "클래스 정원(좌석 수)") int capacity,
        @Schema(description = "ACTIVE: 사용 중 / EXPIRED: 만료(읽기 유예 중일 수 있음)") TicketStatus status,
        LocalDateTime activatedAt,
        LocalDateTime expiresAt,
        @Schema(description = "만료까지 남은 일수. 만료됐으면 0") long remainingDays,
        @Schema(description = "읽기 유예 종료 시각 = expiresAt + 30일") LocalDateTime readGraceEndsAt
) {}

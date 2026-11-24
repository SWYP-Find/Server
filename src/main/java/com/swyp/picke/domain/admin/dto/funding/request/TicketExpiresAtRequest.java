package com.swyp.picke.domain.admin.dto.funding.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Schema(description = "등록된 이용권 만료일 수정 (#691)")
public record TicketExpiresAtRequest(
        @Schema(description = "새 만료 시각 (KST). 등록 시각보다 뒤여야 한다. 지금보다 앞이면 바로 이용 종료", example = "2026-12-08T23:59:59")
        @NotNull LocalDateTime expiresAt
) {
}

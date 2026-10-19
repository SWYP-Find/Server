package com.swyp.picke.domain.admin.dto.funding.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 발급 결과. code 원문은 이 응답에서만 볼 수 있다 (서버는 해시만 저장, 로그 금지).
 * 창을 닫으면 복구할 수 없으므로 전달 전까지 어드민 화면에서 보관해야 한다.
 */
public record TicketIssueResponse(
        List<IssuedTicket> issued,
        List<Skipped> skipped
) {
    public record IssuedTicket(
            Long ticketId,
            Long fundingOrderId,
            String orderId,
            String supporterName,
            @Schema(nullable = true) String supporterEmail,
            @Schema(nullable = true) String supporterPhone,
            int capacity,
            @Schema(description = "등록 기한 (결제 확정일 + 1년)") LocalDateTime registerBy,
            @Schema(description = "이용권 코드 (XXXXXX-XXXXX-XXXXX-XXXXX-XXXXX). 1회만 노출") String code
    ) {}

    public record Skipped(
            Long fundingOrderId,
            @Schema(nullable = true) String orderId,
            @Schema(description = "ALREADY_ISSUED | NOT_ISSUABLE | NOT_FOUND") String reason
    ) {}
}

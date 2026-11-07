package com.swyp.picke.domain.admin.dto.funding.response;

import com.swyp.picke.domain.ticket.enums.FundingPlatform;
import com.swyp.picke.domain.ticket.enums.RegisterReminderStage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "등록기한 임박 알림 대상. 전달 완료·미등록이고 이 단계 알림을 아직 성공 기록하지 않은 이용권, 등록기한 빠른 순")
public record RegisterReminderTargetResponse(
        RegisterReminderStage stage,
        @Schema(description = "대상 수") int count,
        List<Item> items
) {
    public record Item(
            Long ticketId,
            Long fundingOrderId,
            FundingPlatform platform,
            @Schema(description = "플랫폼 주문 번호") String orderId,
            String supporterName,
            @Schema(nullable = true) String supporterEmail,
            @Schema(nullable = true) String supporterPhone,
            int capacity,
            LocalDateTime registerBy,
            @Schema(description = "등록기한까지 남은 일수 (KST 날짜 기준)") long daysLeft
    ) {}
}

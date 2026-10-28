package com.swyp.picke.domain.admin.dto.funding.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "텀블벅 캠페인 관리자 대시보드 통계")
public record TumblbugCampaignDashboardResponse(
        String campaignId,
        @Schema(description = "등록된 후원 주문 수") long supporterCount,
        @Schema(description = "결제 완료 주문 수") long paidConfirmed,
        @Schema(description = "최근 전달 결과가 성공인 주문 수") long delivered,
        @Schema(description = "최근 전달 결과가 성공이 아닌 주문 수") long undelivered
) {
}

package com.swyp.picke.domain.funding.dto.response;

import com.swyp.picke.domain.funding.enums.TumblbugPlan;
import java.time.LocalDateTime;

/** XLSX-only fields attached to the existing admin order response. */
public record TumblbugOrderDetailResponse(
        String campaignName,
        TumblbugPlan plan,
        String supporterNumber,
        String rewardLineId,
        LocalDateTime supportedAt,
        String rewardName,
        String giftText,
        Long rewardAmount,
        Long additionalSupportAmount,
        Long totalSupportAmount,
        Boolean notificationRequested,
        String notificationStatus,
        String paymentStatus,
        String giftDeliveryStatus
) {
}

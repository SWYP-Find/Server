package com.swyp.picke.domain.funding.dto.response;

/** XLSX-only fields attached to the existing admin order response. */
public record TumblbugOrderDetailResponse(
        String campaignName,
        String supporterNumber,
        String rewardLineId,
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

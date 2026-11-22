package com.swyp.picke.domain.funding.dto.request;

import jakarta.validation.constraints.Size;

public record TumblbugSupporterRowInput(
        @Size(max = 120) String supporterNumber,
        @Size(max = 50) String buyerName,
        @Size(max = 300) String giftText,
        @Size(max = 40) String additionalSupportAmount,
        @Size(max = 40) String totalSupportAmount,
        @Size(max = 40) String supportedAt,
        @Size(max = 80) String notificationRequest,
        @Size(max = 80) String paymentStatus,
        @Size(max = 80) String giftDeliveryStatus
) { }

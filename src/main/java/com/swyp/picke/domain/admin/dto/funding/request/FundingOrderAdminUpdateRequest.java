package com.swyp.picke.domain.admin.dto.funding.request;

import com.swyp.picke.domain.funding.enums.DeliveryStatus;
import com.swyp.picke.domain.ticket.enums.RewardTier;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 관리자 펀딩 구매자 편집. 앱 등록 상태는 실제 이용권 등록 상태와 일치해야 합니다. */
public record FundingOrderAdminUpdateRequest(
        @NotBlank @Size(max = 50) String supporterName,
        @Size(max = 320) String contact,
        @NotBlank @Size(max = 50) String orderId,
        @NotNull RewardTier rewardTier,
        @NotNull @Min(1) @Max(7) Integer quantity,
        @NotNull DeliveryStatus deliveryStatus,
        @NotNull AppRegistrationStatus appRegistrationStatus
) {
    public enum AppRegistrationStatus {
        NO_TICKET, APP_PENDING, APP_PARTIAL, APP_REGISTERED
    }
}

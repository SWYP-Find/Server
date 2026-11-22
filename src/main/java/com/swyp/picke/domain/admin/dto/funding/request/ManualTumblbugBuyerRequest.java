package com.swyp.picke.domain.admin.dto.funding.request;

import com.swyp.picke.domain.funding.enums.DeliveryStatus;
import com.swyp.picke.domain.funding.enums.TumblbugPlan;
import com.swyp.picke.domain.ticket.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Schema(description = "텀블벅 펀딩 구매자·주문 수동 등록. 이용권 코드는 별도 발급 절차로 생성합니다.")
public record ManualTumblbugBuyerRequest(
        @NotNull TumblbugPlan plan,
        @NotBlank @Size(max = 50) String campaignId,
        @NotBlank @Size(max = 120) String campaignName,
        @NotBlank @Size(max = 120) String supporterNumber,
        @NotBlank @Size(max = 120) String rewardLineId,
        @NotBlank @Size(max = 50) String buyerName,
        @NotBlank @Size(max = 300) String giftText,
        @NotNull Integer capacity,
        @NotNull LocalDateTime supportedAt,
        @NotNull Boolean notificationRequested,
        @NotNull @PositiveOrZero Long additionalSupportAmount,
        @NotNull @PositiveOrZero Long totalSupportAmount,
        @NotNull PaymentStatus paymentStatus,
        @NotNull DeliveryStatus giftDeliveryStatus
) {
}

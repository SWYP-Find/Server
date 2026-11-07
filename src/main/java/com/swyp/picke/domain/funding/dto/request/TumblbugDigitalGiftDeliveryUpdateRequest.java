package com.swyp.picke.domain.funding.dto.request;

import com.swyp.picke.domain.funding.enums.DeliveryStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "텀블벅 디지털 선물 업로드 결과를 후원번호별로 기록")
public record TumblbugDigitalGiftDeliveryUpdateRequest(
        @NotBlank @Size(max = 120) String campaignId,
        @NotBlank @Size(max = 120) String rewardLineId,
        @NotEmpty @Size(max = 1000) List<@NotBlank @Size(max = 120) String> supporterNumbers,
        @NotNull @Schema(description = "텀블벅 업로드 결과. PENDING은 입력할 수 없습니다.", example = "SENT") DeliveryStatus status,
        @Size(max = 500) @Schema(description = "FAILED 처리 시 필수", example = "텀블벅 업로드 결과에서 후원번호가 일치하지 않음") String failedReason
) {
}

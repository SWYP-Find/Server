package com.swyp.picke.domain.admin.dto.ad.request;

import com.swyp.picke.domain.ad.enums.AdStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "광고 소재 게재 상태 변경 요청")
public record AdStatusRequest(

        @Schema(description = "변경할 게재 상태", example = "PAUSED")
        @NotNull(message = "게재 상태는 필수입니다.")
        AdStatus status
) {
}

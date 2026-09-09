package com.swyp.picke.domain.admin.dto.ad.response;

import com.swyp.picke.domain.ad.enums.AdNetwork;
import com.swyp.picke.domain.ad.enums.AdSlotCode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "광고 클릭 내역 한 건")
public record AdClickLogResponse(

        Long clickId,

        @Schema(description = "소재 코드", example = "a1b2c3d4")
        String code,

        @Schema(description = "소재 제목")
        String title,

        AdNetwork network,

        AdSlotCode slot,

        @Schema(description = "클릭 시각")
        LocalDateTime clickedAt
) {
}

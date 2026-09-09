package com.swyp.picke.domain.admin.dto.ad.response;

import com.swyp.picke.domain.ad.enums.AdNetwork;
import com.swyp.picke.domain.ad.enums.AdSlotCode;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "소재별 노출/클릭 집계")
public record AdStatsResponse(
        Long creativeId,
        String code,
        AdNetwork network,
        AdSlotCode slot,
        String title,

        @Schema(description = "기간 내 노출 수")
        long impressions,

        @Schema(description = "기간 내 클릭 수")
        long clicks,

        @Schema(description = "클릭률(%). 노출이 0이면 0", example = "1.25")
        double ctr
) {

    public static AdStatsResponse of(Long creativeId, String code, AdNetwork network, AdSlotCode slot,
                                     String title, long impressions, long clicks) {
        double ctr = impressions == 0 ? 0d : Math.round(clicks * 10000d / impressions) / 100d;
        return new AdStatsResponse(creativeId, code, network, slot, title, impressions, clicks, ctr);
    }
}

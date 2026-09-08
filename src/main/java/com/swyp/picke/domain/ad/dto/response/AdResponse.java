package com.swyp.picke.domain.ad.dto.response;

import com.swyp.picke.domain.ad.entity.AdCreative;
import com.swyp.picke.domain.ad.enums.AdNetwork;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "앱 지면에 노출할 제휴 광고 소재")
public record AdResponse(

        @Schema(description = "소재 코드", example = "a1b2c3d4")
        String code,

        @Schema(description = "매체", example = "COUPANG")
        AdNetwork network,

        @Schema(description = "배너 주 문구", example = "지금 인기 있는 무선 이어폰")
        String title,

        @Schema(description = "배너 보조 문구", example = "리뷰 1만 개 이상")
        String subtitle,

        @Schema(description = "소재 이미지 URL")
        String imageUrl,

        @Schema(description = "버튼 문구", example = "구매하러 가기")
        String ctaText,

        @Schema(description = "탭 시 이동할 URL. 외부 브라우저로 열어야 한다.",
                example = "https://ad.picke.store/c/a1b2c3d4")
        String clickUrl,

        @Schema(description = "광고 표기 라벨. 표시광고법 대응이므로 반드시 렌더해야 한다.", example = "광고")
        String label
) {

    private static final String AD_LABEL = "광고";

    public static AdResponse of(AdCreative creative, String clickUrl) {
        return new AdResponse(
                creative.getCode(),
                creative.getNetwork(),
                creative.getTitle(),
                creative.getSubtitle(),
                creative.getImageUrl(),
                creative.getCtaText(),
                clickUrl,
                AD_LABEL
        );
    }
}

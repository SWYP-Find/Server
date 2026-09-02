package com.swyp.picke.domain.ad.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * 애드픽 캠페인 리스트 API(offers.php) 응답 한 건.
 * 문서화되지 않은 필드가 늘어날 수 있어 모르는 필드는 무시한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AdpickCampaignResponse(

        @JsonProperty("apOffer") String offerId,
        @JsonProperty("apAppTitle") String appTitle,
        @JsonProperty("apHeadline") String headline,
        @JsonProperty("apAppPromoText") String promoText,
        @JsonProperty("apImages") Map<String, String> images,
        @JsonProperty("apTrackingLink") String trackingLink,
        @JsonProperty("apOS") String os,
        @JsonProperty("apRemain") Integer remain
) {

    private static final String ICON_KEY = "icon";

    public String iconUrl() {
        return images == null ? null : images.get(ICON_KEY);
    }

    /** 잔여 수량이 없는 캠페인은 클릭해도 전환이 잡히지 않으므로 노출하지 않는다. */
    public boolean hasRemaining() {
        return remain != null && remain > 0;
    }

    /** 이미지나 추적 링크가 비면 배너를 그릴 수 없다. */
    public boolean isRenderable() {
        return offerId != null && !offerId.isBlank()
                && appTitle != null && !appTitle.isBlank()
                && trackingLink != null && !trackingLink.isBlank()
                && iconUrl() != null;
    }
}

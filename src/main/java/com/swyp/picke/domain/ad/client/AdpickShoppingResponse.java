package com.swyp.picke.domain.ad.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 애드픽 쇼핑·핫딜 API(sdk_shopping.php, sdk_shopping_hotdeal.php)의 상품 한 건.
 *
 * <p>앱 설치형 캠페인과 달리 상품에는 애드픽이 주는 고유 코드가 없다.
 * {@code buyurl} 이 이미 affid 가 박힌 완성된 추적 링크라, 이 값을 식별자의 근거로 삼는다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AdpickShoppingResponse(

        @JsonProperty("product_name") String productName,
        @JsonProperty("photo") String photo,
        @JsonProperty("mall_name") String mallName,
        @JsonProperty("commission") String commission,
        @JsonProperty("buyurl") String buyUrl
) {

    /** 이미지나 구매 링크가 비면 배너를 그릴 수 없다. */
    public boolean isRenderable() {
        return productName != null && !productName.isBlank()
                && photo != null && !photo.isBlank()
                && buyUrl != null && !buyUrl.isBlank();
    }

    /** 판매처와 수수료를 보조 문구로 쓴다. 둘 다 없으면 보조 문구를 비운다. */
    public String subtitle() {
        if (mallName == null || mallName.isBlank()) {
            return commission == null || commission.isBlank() ? null : commission;
        }
        return commission == null || commission.isBlank()
                ? mallName
                : mallName + " · " + commission;
    }
}

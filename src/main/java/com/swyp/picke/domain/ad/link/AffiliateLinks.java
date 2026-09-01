package com.swyp.picke.domain.ad.link;

import org.springframework.web.util.UriComponentsBuilder;

/**
 * 제휴 링크에 추적 파라미터를 끼워 넣는 공통 규칙.
 */
final class AffiliateLinks {

    private AffiliateLinks() {
    }

    /**
     * 이미 쿼리스트링이 붙어 있는 제휴 링크에 파라미터를 병합한다.
     *
     * <p>단순 문자열 결합이 아니다. 같은 이름의 파라미터가 이미 있으면 우리 값으로 덮는다.
     * build(true)로 두는 이유는 landingUrl이 각 매체 콘솔에서 인코딩까지 끝난 상태로 오기 때문이다.
     * 여기서 다시 인코딩하면 이중 인코딩된다.
     */
    static String merge(String landingUrl, String paramName, String value) {
        return UriComponentsBuilder.fromUriString(landingUrl)
                .replaceQueryParam(paramName, value)
                .build(true)
                .toUriString();
    }

    /** 지면별 성과를 가르기 위한 추적값. 영문·숫자·밑줄만 쓰므로 인코딩이 필요 없다. */
    static String subIdOf(com.swyp.picke.domain.ad.entity.AdCreative creative) {
        return creative.getSlot().name() + "_" + creative.getCode();
    }
}

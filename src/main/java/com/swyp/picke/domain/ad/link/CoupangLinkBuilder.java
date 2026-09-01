package com.swyp.picke.domain.ad.link;

import com.swyp.picke.domain.ad.entity.AdCreative;
import com.swyp.picke.domain.ad.enums.AdNetwork;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 쿠팡 파트너스는 subId를 지원한다. 지면별로 값을 달리 넣으면 파트너스 리포트에서
 * 지면별 실매출이 갈려, 어느 지면이 돈이 되는지 데이터로 볼 수 있다.
 */
@Component
public class CoupangLinkBuilder implements AffiliateLinkBuilder {

    private static final String SUB_ID_PARAM = "subId";

    @Override
    public AdNetwork network() {
        return AdNetwork.COUPANG;
    }

    @Override
    public String build(AdCreative creative) {
        String subId = creative.getSlot().name() + "_" + creative.getCode();

        // build(true): landingUrl은 콘솔에서 발급된 인코딩 완료 상태라 재인코딩하면 이중 인코딩된다.
        return UriComponentsBuilder.fromUriString(creative.getLandingUrl())
                .replaceQueryParam(SUB_ID_PARAM, subId)
                .build(true)
                .toUriString();
    }
}

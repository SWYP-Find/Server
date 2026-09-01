package com.swyp.picke.domain.ad.link;

import com.swyp.picke.domain.ad.entity.AdCreative;
import com.swyp.picke.domain.ad.enums.AdNetwork;
import org.springframework.stereotype.Component;

/**
 * 애드픽은 현재 pass-through다.
 *
 * <p>서브아이디 파라미터 규격을 아직 확인하지 못했다. 파트너센터 링크생성 화면에서 확인한 뒤
 * 쿠팡과 같은 방식으로 병합하면 된다. 그전까지 애드픽은 지면별 성과 분리만 안 될 뿐,
 * 노출·클릭·리다이렉트는 정상 동작한다.
 */
@Component
public class AdpickLinkBuilder implements AffiliateLinkBuilder {

    @Override
    public AdNetwork network() {
        return AdNetwork.ADPICK;
    }

    @Override
    public String build(AdCreative creative) {
        return creative.getLandingUrl();
    }
}

package com.swyp.picke.domain.ad.link;

import com.swyp.picke.domain.ad.entity.AdCreative;
import com.swyp.picke.domain.ad.enums.AdNetwork;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 애드픽 서브아이디 파라미터명은 설정값으로 둔다.
 * 파트너센터 링크생성 화면에서 규격을 확인하기 전까지는 값이 비어 있고, 그동안은 pass-through로
 * 원본 링크를 그대로 넘긴다. 지면별 성과 분리만 안 될 뿐 노출·클릭·리다이렉트는 정상 동작한다.
 * 규격이 확인되면 배포 없이 {@code picke.ad.adpick.sub-id-param} 만 채우면 쿠팡과 같은 방식으로 붙는다.
 */
@Component
public class AdpickLinkBuilder implements AffiliateLinkBuilder {

    @Value("${picke.ad.adpick.sub-id-param:}")
    private String subIdParam;

    @Override
    public AdNetwork network() {
        return AdNetwork.ADPICK;
    }

    @Override
    public String build(AdCreative creative) {
        if (!StringUtils.hasText(subIdParam)) {
            return creative.getLandingUrl();
        }
        return AffiliateLinks.merge(creative.getLandingUrl(), subIdParam, AffiliateLinks.subIdOf(creative));
    }
}

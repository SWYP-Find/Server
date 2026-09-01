package com.swyp.picke.domain.ad.link;

import com.swyp.picke.domain.ad.entity.AdCreative;
import com.swyp.picke.domain.ad.enums.AdNetwork;

/**
 * 매체별 최종 이동 URL을 만든다.
 *
 * <p>소재의 landingUrl은 각 매체 콘솔에서 발급한 완성형 링크라 이미 쿼리스트링을 갖고 있다.
 * 추적 파라미터는 문자열 결합이 아니라 병합이어야 한다.
 */
public interface AffiliateLinkBuilder {

    AdNetwork network();

    String build(AdCreative creative);
}

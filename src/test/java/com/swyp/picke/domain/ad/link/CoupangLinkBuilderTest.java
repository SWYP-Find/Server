package com.swyp.picke.domain.ad.link;

import com.swyp.picke.domain.ad.entity.AdCreative;
import com.swyp.picke.domain.ad.enums.AdNetwork;
import com.swyp.picke.domain.ad.enums.AdSlotCode;
import com.swyp.picke.domain.ad.enums.AdStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CoupangLinkBuilderTest {

    private final CoupangLinkBuilder builder = new CoupangLinkBuilder();

    private AdCreative creative(String landingUrl) {
        return AdCreative.builder()
                .code("abc12345")
                .network(AdNetwork.COUPANG)
                .slot(AdSlotCode.HOME_FEED)
                .title("무선 이어폰")
                .imageUrl("https://img.example.com/1.jpg")
                .ctaText("구매하러 가기")
                .landingUrl(landingUrl)
                .status(AdStatus.ACTIVE)
                .weight(1)
                .build();
    }

    @Test
    @DisplayName("쿼리스트링이 이미 있는 제휴 링크에도 subId를 병합한다")
    void build_mergesSubIdIntoExistingQueryString() {
        String url = builder.build(creative("https://link.coupang.com/re/AFF?lptag=AF6830373&pageKey=123"));

        assertThat(url).contains("lptag=AF6830373");
        assertThat(url).contains("pageKey=123");
        assertThat(url).contains("subId=HOME_FEED_abc12345");
        assertThat(url).doesNotContain("??");
    }

    @Test
    @DisplayName("쿼리스트링이 없는 링크에는 subId를 새로 붙인다")
    void build_appendsSubIdWhenNoQueryString() {
        String url = builder.build(creative("https://link.coupang.com/a/abcdef"));

        assertThat(url).isEqualTo("https://link.coupang.com/a/abcdef?subId=HOME_FEED_abc12345");
    }

    @Test
    @DisplayName("이미 subId가 있으면 우리 값으로 덮어쓴다")
    void build_replacesExistingSubId() {
        String url = builder.build(creative("https://link.coupang.com/a/abcdef?subId=old"));

        assertThat(url).contains("subId=HOME_FEED_abc12345");
        assertThat(url).doesNotContain("subId=old");
    }

    @Test
    @DisplayName("인코딩된 파라미터를 이중 인코딩하지 않는다")
    void build_doesNotDoubleEncode() {
        String url = builder.build(creative("https://link.coupang.com/a/x?q=%EC%9D%B4%EC%96%B4%ED%8F%B0"));

        assertThat(url).contains("q=%EC%9D%B4%EC%96%B4%ED%8F%B0");
        assertThat(url).doesNotContain("%25");
    }
}

package com.swyp.picke.domain.ad.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swyp.picke.domain.ad.enums.AdTargetOs;
import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 애드픽 offers.php 실제 응답으로 역직렬화를 확인한다.
 * 픽스처는 실제 호출 결과를 그대로 옮긴 것이고, 잔여 0건과 아이콘 누락 건만 필터 경로 확인용으로 파생시켰다.
 */
class AdpickCampaignResponseTest {

    private static List<AdpickCampaignResponse> campaigns;

    @BeforeAll
    static void parseFixture() throws Exception {
        try (InputStream in = AdpickCampaignResponseTest.class
                .getResourceAsStream("/fixtures/ad/adpick-offers.json")) {
            campaigns = new ObjectMapper().readValue(in, new TypeReference<>() {
            });
        }
    }

    @Test
    @DisplayName("실제 응답의 필드가 우리가 쓰는 이름으로 모두 들어온다")
    void deserializesFieldsWeUse() {
        AdpickCampaignResponse first = campaigns.get(0);

        assertThat(first.offerId()).isEqualTo("9d3a9");
        assertThat(first.appTitle()).isNotBlank();
        assertThat(first.headline()).isNotBlank();
        assertThat(first.trackingLink()).startsWith("https://");
        assertThat(first.os()).isEqualTo("Both");
        assertThat(first.remain()).isPositive();
        assertThat(first.iconUrl()).startsWith("https://");
    }

    @Test
    @DisplayName("문서에 없는 필드가 섞여 들어와도 파싱이 깨지지 않는다")
    void ignoresUndocumentedFields() {
        // 실제 응답에는 apKPI·apHook·apEvent 등 우리가 쓰지 않는 필드가 함께 온다.
        assertThat(campaigns).hasSize(6);
    }

    /**
     * 실제 응답에 apOS 가 null 인 캠페인이 섞여 온다. OS 를 가리지 않는 것으로 보고 전체에 노출한다.
     * iOS 전용 캠페인은 아직 실제 응답에서 관측하지 못해, iOS 매핑은 AdTargetOsTest 의 문자열 규칙으로만 덮여 있다.
     */
    @Test
    @DisplayName("apOS가 null인 실제 캠페인은 전체 노출로 본다")
    void mapsNullOsToAll() {
        AdpickCampaignResponse nullOs = campaigns.get(5);

        assertThat(nullOs.os()).isNull();
        assertThat(AdTargetOs.fromAdpick(nullOs.os())).isEqualTo(AdTargetOs.ALL);
    }

    @Test
    @DisplayName("apOS 실제 값 Both·Android가 타깃 OS로 옮겨진다")
    void mapsRealOsValues() {
        assertThat(AdTargetOs.fromAdpick(campaigns.get(0).os())).isEqualTo(AdTargetOs.ALL);
        assertThat(AdTargetOs.fromAdpick(campaigns.get(1).os())).isEqualTo(AdTargetOs.ANDROID);
    }

    @Test
    @DisplayName("헤드라인·프로모션 문구가 모두 빈 캠페인도 렌더 대상이다. 보조 문구만 비게 둔다")
    void treatsEmptyCopyAsRenderable() {
        AdpickCampaignResponse noCopy = campaigns.get(2);

        assertThat(noCopy.headline()).isEmpty();
        assertThat(noCopy.promoText()).isEmpty();
        assertThat(noCopy.isRenderable()).isTrue();
    }

    @Test
    @DisplayName("잔여가 0이면 게재 대상이 아니다")
    void marksSoldOutCampaignAsNotServable() {
        assertThat(campaigns.get(3).hasRemaining()).isFalse();
    }

    @Test
    @DisplayName("아이콘이 없으면 배너를 그릴 수 없으므로 렌더 대상에서 뺀다")
    void excludesCampaignWithoutIcon() {
        assertThat(campaigns.get(4).iconUrl()).isNull();
        assertThat(campaigns.get(4).isRenderable()).isFalse();
    }
}

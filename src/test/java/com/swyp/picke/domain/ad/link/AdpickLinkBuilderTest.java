package com.swyp.picke.domain.ad.link;

import com.swyp.picke.domain.ad.entity.AdCreative;
import com.swyp.picke.domain.ad.enums.AdNetwork;
import com.swyp.picke.domain.ad.enums.AdSlotCode;
import com.swyp.picke.domain.ad.enums.AdStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class AdpickLinkBuilderTest {

    private static final String LANDING = "https://adpick.co.kr/?ac=offer&tac=campaign&id=123";

    private AdCreative creative() {
        return AdCreative.builder()
                .code("abc12345")
                .network(AdNetwork.ADPICK)
                .slot(AdSlotCode.BATTLE_RESULT_BOTTOM)
                .title("앱 설치하고 포인트 받기")
                .imageUrl("https://img.example.com/1.jpg")
                .ctaText("설치하고 받기")
                .landingUrl(LANDING)
                .status(AdStatus.ACTIVE)
                .weight(1)
                .build();
    }

    private AdpickLinkBuilder builder(String subIdParam) {
        AdpickLinkBuilder builder = new AdpickLinkBuilder();
        ReflectionTestUtils.setField(builder, "subIdParam", subIdParam);
        return builder;
    }

    @Test
    @DisplayName("파라미터명이 비어 있으면 원본 링크를 그대로 넘긴다")
    void build_passThroughWhenParamNotConfigured() {
        assertThat(builder("").build(creative())).isEqualTo(LANDING);
        assertThat(builder(null).build(creative())).isEqualTo(LANDING);
    }

    @Test
    @DisplayName("파라미터명을 채우면 배포 없이 지면별 추적값이 붙는다")
    void build_mergesSubIdWhenConfigured() {
        String url = builder("subid").build(creative());

        assertThat(url).contains("subid=BATTLE_RESULT_BOTTOM_abc12345");
        assertThat(url).contains("ac=offer");
        assertThat(url).contains("id=123");
    }
}

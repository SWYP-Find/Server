package com.swyp.picke.domain.ad.controller;

import com.swyp.picke.domain.ad.entity.AdCreative;
import com.swyp.picke.domain.ad.enums.AdNetwork;
import com.swyp.picke.domain.ad.enums.AdSlotCode;
import com.swyp.picke.domain.ad.enums.AdStatus;
import com.swyp.picke.domain.ad.enums.AdTargetOs;
import com.swyp.picke.domain.ad.repository.AdCreativeRepository;
import com.swyp.picke.domain.ad.repository.AdImpressionDailyRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 제휴 광고 배너가 앱까지 실제로 나가는지 확인한다.
 * 조회 → 노출 집계 → 클릭 리다이렉트 → 공개 랜딩까지 한 흐름으로 본다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdApiIntegrationTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdCreativeRepository adCreativeRepository;

    @Autowired
    private AdImpressionDailyRepository adImpressionDailyRepository;

    @BeforeEach
    void setUp() {
        adImpressionDailyRepository.deleteAll();
        adCreativeRepository.deleteAll();
    }

    @Test
    @DisplayName("게재 중인 쿠팡 소재는 배너 렌더에 필요한 값과 클릭 URL을 모두 담아 나간다")
    void getAds_returnsServableCoupangCreative() throws Exception {
        adCreativeRepository.save(coupang("cpg00001", AdSlotCode.HOME_FEED, AdTargetOs.ALL, AdStatus.ACTIVE));

        mockMvc.perform(get("/api/v1/ads").param("slot", "HOME_FEED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].code").value("cpg00001"))
                .andExpect(jsonPath("$.data[0].network").value("COUPANG"))
                .andExpect(jsonPath("$.data[0].title").value("무선 이어폰"))
                .andExpect(jsonPath("$.data[0].imageUrl").value("https://img.example.com/1.jpg"))
                .andExpect(jsonPath("$.data[0].ctaText").value("구매하러 가기"))
                .andExpect(jsonPath("$.data[0].clickUrl").value("https://ad.picke.store/c/cpg00001"))
                .andExpect(jsonPath("$.data[0].label").value("광고"));
    }

    @Test
    @DisplayName("게재 가능한 소재가 없으면 빈 배열을 준다. 앱은 이때 지면을 숨긴다")
    void getAds_returnsEmptyWhenNothingServable() throws Exception {
        adCreativeRepository.save(coupang("cpg00002", AdSlotCode.HOME_FEED, AdTargetOs.ALL, AdStatus.PAUSED));

        mockMvc.perform(get("/api/v1/ads").param("slot", "HOME_FEED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("iOS 요청에 Android 전용 애드픽 소재는 나가지 않는다")
    void getAds_excludesCreativeOfOtherOs() throws Exception {
        adCreativeRepository.save(coupang("apk00001", AdSlotCode.BATTLE_RESULT_BOTTOM,
                AdTargetOs.ANDROID, AdStatus.ACTIVE));

        mockMvc.perform(get("/api/v1/ads")
                        .param("slot", "BATTLE_RESULT_BOTTOM")
                        .param("os", "IOS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        mockMvc.perform(get("/api/v1/ads")
                        .param("slot", "BATTLE_RESULT_BOTTOM")
                        .param("os", "ANDROID"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @DisplayName("노출 집계는 같은 소재를 두 번 보내면 같은 날짜 행에 누적된다")
    void recordImpressions_accumulatesIntoSameDailyRow() throws Exception {
        AdCreative creative = adCreativeRepository.save(
                coupang("cpg00003", AdSlotCode.HOME_FEED, AdTargetOs.ALL, AdStatus.ACTIVE));

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/v1/ads/impressions")
                            .contentType("application/json")
                            .content("{\"codes\":[\"cpg00003\"]}"))
                    .andExpect(status().isOk());
        }

        assertThat(adImpressionDailyRepository
                .findByCreativeIdAndSlotAndStatDate(creative.getId(), AdSlotCode.HOME_FEED, LocalDate.now(KST)))
                .isPresent()
                .get()
                .extracting(daily -> daily.getImpressions())
                .isEqualTo(2L);
    }

    @Test
    @DisplayName("노출 집계는 한 번에 밀어 넣을 수 있는 소재 수를 제한한다")
    void recordImpressions_rejectsOversizedBatch() throws Exception {
        String codes = java.util.stream.IntStream.range(0, 21)
                .mapToObj(i -> "\"code%02d\"".formatted(i))
                .collect(java.util.stream.Collectors.joining(","));

        mockMvc.perform(post("/api/v1/ads/impressions")
                        .contentType("application/json")
                        .content("{\"codes\":[" + codes + "]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("클릭 숏링크는 지면별 subId를 붙인 제휴 링크로 리다이렉트한다")
    void click_redirectsToAffiliateLinkWithSubId() throws Exception {
        adCreativeRepository.save(coupang("cpg00004", AdSlotCode.HOME_FEED, AdTargetOs.ALL, AdStatus.ACTIVE));

        mockMvc.perform(get("/c/cpg00004"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location",
                        "https://link.coupang.com/a/ABCDE?lptag=AF6830373&subId=HOME_FEED_cpg00004"));
    }

    @Test
    @DisplayName("없는 코드로 들어오면 404 대신 광고 랜딩으로 흘려보낸다")
    void click_fallsBackToLandingWhenCodeMissing() throws Exception {
        mockMvc.perform(get("/c/nosuchcode"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "https://ad.picke.store/"));
    }

    @Test
    @DisplayName("ad.picke.store 루트는 매체 심사용 공개 페이지에 소재를 그린다")
    void landing_rendersCreativesOnAdHost() throws Exception {
        adCreativeRepository.save(coupang("cpg00005", AdSlotCode.HOME_FEED, AdTargetOs.ALL, AdStatus.ACTIVE));

        mockMvc.perform(get("/").with(request -> {
                    request.setServerName("ad.picke.store");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("무선 이어폰")))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("https://ad.picke.store/c/cpg00005")));
    }

    @Test
    @DisplayName("광고 도메인이 아닌 Host의 루트에는 광고 페이지가 뜨지 않는다")
    void landing_doesNotRenderOnApiHost() throws Exception {
        adCreativeRepository.save(coupang("cpg00006", AdSlotCode.HOME_FEED, AdTargetOs.ALL, AdStatus.ACTIVE));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string("PICKE"));
    }

    private AdCreative coupang(String code, AdSlotCode slot, AdTargetOs targetOs, AdStatus status) {
        return AdCreative.builder()
                .code(code)
                .network(AdNetwork.COUPANG)
                .slot(slot)
                .title("무선 이어폰")
                .subtitle("리뷰 1만 개 이상")
                .imageUrl("https://img.example.com/1.jpg")
                .ctaText("구매하러 가기")
                .landingUrl("https://link.coupang.com/a/ABCDE?lptag=AF6830373")
                .status(status)
                .weight(1)
                .targetOs(targetOs)
                .build();
    }
}

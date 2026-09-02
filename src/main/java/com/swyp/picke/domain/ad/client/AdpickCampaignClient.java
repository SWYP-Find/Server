package com.swyp.picke.domain.ad.client;

import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 애드픽 캠페인 리스트 API 호출.
 * 애드픽 가이드가 최대 1분에 1회 이하 호출을 요구하므로 요청마다 부르지 않고 스케줄러로만 부른다.
 * 실제로 짧은 간격으로 연달아 호출하면 403 을 돌려준다.
 */
@Slf4j
@Component
public class AdpickCampaignClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    @Value("${picke.ad.adpick.base-url:https://adpick.co.kr/apis/offers.php}")
    private String baseUrl;

    @Value("${picke.ad.adpick.aff-id:}")
    private String affId;

    /** 가입 전에는 affId 가 비어 있다. 그때는 동기화를 건너뛴다. */
    public boolean isConfigured() {
        return StringUtils.hasText(affId);
    }

    public List<AdpickCampaignResponse> fetchCampaigns() {
        String url = UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("affid", affId)
                .queryParam("order", "rand")
                .build()
                .toUriString();

        List<AdpickCampaignResponse> campaigns = WebClient.create()
                .get()
                .uri(url)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<AdpickCampaignResponse>>() {
                })
                .timeout(TIMEOUT)
                .block();

        return campaigns == null ? List.of() : campaigns;
    }
}

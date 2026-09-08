package com.swyp.picke.domain.ad.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 애드픽 쇼핑·핫딜 상품을 가져온다.
 *
 * <p>캠페인 리스트 API(offers.php)와 별도 재고다. 이 계정 기준으로 앱 캠페인은 7건뿐이지만
 * 쇼핑과 핫딜에서 각각 30건이 더 내려온다. 지면을 채우려면 이쪽이 필요하다.
 *
 * <p>두 API는 응답이 {@code {title, description, list:[...]}} 로 한 겹 싸여 있다.
 * 형태가 같아 같은 파서를 쓴다.
 */
@Slf4j
@Component
public class AdpickShoppingClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final String shoppingUrl;
    private final String hotdealUrl;
    private final String affId;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AdpickShoppingClient(
            @Value("${picke.ad.adpick.shopping-url:https://adpick.co.kr/apis/sdk_shopping.php}") String shoppingUrl,
            @Value("${picke.ad.adpick.hotdeal-url:https://adpick.co.kr/apis/sdk_shopping_hotdeal.php}") String hotdealUrl,
            @Value("${picke.ad.adpick.aff-id:}") String affId) {
        this.shoppingUrl = shoppingUrl;
        this.hotdealUrl = hotdealUrl;
        this.affId = affId;
    }

    public boolean isConfigured() {
        return StringUtils.hasText(affId);
    }

    /** 쇼핑과 핫딜을 합쳐 준다. 한쪽이 실패해도 다른 쪽은 살린다. */
    public List<AdpickShoppingResponse> fetchProducts() {
        List<AdpickShoppingResponse> products = new ArrayList<>();
        products.addAll(fetchFrom(shoppingUrl));
        products.addAll(fetchFrom(hotdealUrl));
        return products;
    }

    private List<AdpickShoppingResponse> fetchFrom(String baseUrl) {
        String url = UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("affid", affId)
                .build()
                .toUriString();
        try {
            // 이 API 는 JSON 을 text/html 로 내려준다. 컨텐츠 타입만 믿고 디코딩하면 전부 실패한다.
            String body = WebClient.create()
                    .get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(TIMEOUT)
                    .block();

            if (body == null || body.isBlank()) {
                return List.of();
            }
            List<Group> groups = objectMapper.readValue(body, new TypeReference<List<Group>>() {
            });
            return groups.stream()
                    .filter(group -> group.list() != null)
                    .flatMap(group -> group.list().stream())
                    .toList();
        } catch (Exception e) {
            // 재고를 늘리려고 붙인 부가 피드다. 여기서 터져도 앱 캠페인 동기화까지 막지 않는다.
            log.warn("[AdpickShopping] {} 조회 실패: {}", baseUrl, e.getMessage());
            return List.of();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Group(@JsonProperty("list") List<AdpickShoppingResponse> list) {
    }
}

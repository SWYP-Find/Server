package com.swyp.picke.domain.classroom.service.report;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * OpenAI Moderation 호출. 입력이 걸린 범주(categories 중 true) 이름만 돌려주고, 어떤 범주를 막을지는 호출하는 쪽이 정한다.
 * Moderation 은 과금되지 않으므로 ai_jobs 에 기록하지 않는다.
 */
@Slf4j
@Component
public class ClassModerationClient {

    private static final int CONNECT_TIMEOUT_MS = 3_000;
    private static final int READ_TIMEOUT_MS = 10_000;

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.moderation.url:https://api.openai.com/v1/moderations}")
    private String moderationUrl;

    @Value("${openai.moderation.model:omni-moderation-latest}")
    private String model;

    /** 걸린 범주 이름 (예: harassment/threatening). 걸린 것이 없으면 빈 집합 */
    public Set<String> flaggedCategories(String text) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        RestClient restClient = RestClient.builder().requestFactory(factory).build();

        Map<String, Object> response = restClient.post()
                .uri(moderationUrl)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(Map.of("model", model, "input", text))
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {
                });

        if (response == null) {
            throw new IllegalStateException("empty response from moderation");
        }
        List<Map<String, Object>> results = ReportContent.mapList(response, "results");
        if (results.isEmpty()) {
            throw new IllegalStateException("no results in moderation response");
        }
        Map<String, Object> categories = ReportContent.map(results.get(0), "categories");
        Set<String> flagged = new LinkedHashSet<>();
        if (categories != null) {
            categories.forEach((name, value) -> {
                if (Boolean.TRUE.equals(value)) {
                    flagged.add(name);
                }
            });
        }
        return flagged;
    }
}

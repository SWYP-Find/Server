package com.swyp.picke.domain.perspective.service;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.entity.BattleOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GptPerspectiveGenerationService {

    private static final String SYSTEM_PROMPT =
            "당신은 철학적 토론 배틀에서 특정 입장을 지지하는 관점을 작성하는 AI입니다. " +
            "주어진 배틀 주제와 옵션 정보를 바탕으로, 해당 옵션을 지지하는 자신만의 관점을 한국어로 100자 이내로 작성하세요. " +
            "자연스럽고 간결하며, 일반인이 직접 쓴 것처럼 작성하세요. 관점 텍스트만 출력하세요.";

    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 15000;

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.url}")
    private String openaiUrl;

    @Value("${openai.model}")
    private String model;

    public String generate(Battle battle, BattleOption option) {
        String userPrompt = String.format(
                "배틀 제목: %s\n배틀 설명: %s\n지지할 옵션: %s\n해당 옵션의 입장: %s",
                battle.getTitle(), battle.getDescription(), option.getTitle(), option.getStance()
        );

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        RestClient restClient = RestClient.builder().requestFactory(factory).build();

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "max_tokens", 200
        );

        Map response = restClient.post()
                .uri(openaiUrl)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(requestBody)
                .retrieve()
                .body(Map.class);

        List choices = (List) response.get("choices");
        Map choice = (Map) choices.get(0);
        Map message = (Map) choice.get("message");
        return ((String) message.get("content")).trim();
    }
}

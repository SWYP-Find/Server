package com.swyp.picke.domain.battle.thumbnail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swyp.picke.domain.admin.dto.battle.request.AdminBattleThumbnailCandidateRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 배틀 제목(주제)을 썸네일로 그릴 상징적인 장면 묘사로 바꾼다. A/B 입장이 아니라 제목 자체를 그린다.
 * 토론 원문(안락사, 전쟁 등)을 이미지 프롬프트에 그대로 넣지 않고, 위해를 직접 그리지 않는 은유적 장면으로 옮겨
 * 후보마다 서로 다른 장면을 쓰게 한다. {@code OpenAiEmotionClassifier} 와 같은 방식(RestClient + openai.* 설정)을 쓴다.
 */
@Slf4j
@Component
public class ThumbnailSceneWriter {

    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 30000;

    private static final String SYSTEM = """
            너는 철학 토론 배틀의 썸네일 일러스트 장면을 기획하는 아트 디렉터다.
            배틀 제목이 다루는 주제(대상, 상황, 질문) 자체를 상징적으로 보여주는 장면을 서로 다르게 %d개 묘사해라.
            한 줄 요약과 설명은 제목의 의미를 파악하는 맥락으로만 쓴다.
            규칙:
            - 찬반 두 입장을 나눠 그리거나, 화면을 좌우로 분할하거나, 두 대상이 맞서는 대립 구도로 만들지 않는다.
            - 각 장면은 2~3문장의 한국어로, 화면에 무엇이 어디에 어떻게 보이는지 구체적으로 쓴다.
            - 실루엣, 사물, 빛과 그림자, 공간, 자연물 같은 은유로 표현한다.
            - 부상, 피, 시신, 무기 사용, 자해, 죽어가는 사람 등 위해를 직접 묘사하지 않는다.
            - 텍스트, 글자, 숫자, 로고는 장면에 넣지 않는다.
            - 화풍이나 색상은 쓰지 않는다(별도로 지정된다).
            반드시 JSON 만 출력: {"scenes":["...","..."]}
            """;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.url}")
    private String openaiUrl;

    @Value("${openai.model}")
    private String model;

    /** 실패하면 빈 목록을 돌려준다. 호출부는 배틀 원문으로 대체한다. */
    public List<String> write(AdminBattleThumbnailCandidateRequest request, int count) {
        try {
            JsonNode root = call(SYSTEM.formatted(count), describeBattle(request));
            List<String> scenes = new ArrayList<>();
            for (JsonNode scene : root.path("scenes")) {
                String text = scene.asText("").trim();
                if (!text.isEmpty()) {
                    scenes.add(text);
                }
            }
            return scenes;
        } catch (Exception e) {
            log.warn("[Thumbnail] 장면 묘사 생성 실패 - 배틀 원문으로 생성", e);
            return List.of();
        }
    }

    static String describeBattle(AdminBattleThumbnailCandidateRequest request) {
        return "제목: " + orEmpty(request.title())
                + "\n한 줄 요약: " + orEmpty(request.summary())
                + "\n설명: " + orEmpty(request.description());
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private JsonNode call(String systemPrompt, String userPrompt) throws Exception {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        RestClient restClient = RestClient.builder().requestFactory(factory).build();

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)));

        Map<?, ?> response = restClient.post()
                .uri(openaiUrl)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(requestBody)
                .retrieve()
                .body(Map.class);

        List<?> choices = (List<?>) response.get("choices");
        Map<?, ?> choice = (Map<?, ?>) choices.get(0);
        Map<?, ?> message = (Map<?, ?>) choice.get("message");
        return objectMapper.readTree((String) message.get("content"));
    }
}

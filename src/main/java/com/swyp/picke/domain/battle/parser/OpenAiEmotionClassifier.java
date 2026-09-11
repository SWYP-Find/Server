package com.swyp.picke.domain.battle.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swyp.picke.domain.scenario.enums.Tone;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * OpenAI Chat Completions 로 감정 톤/효과 태그를 한 번에 배치 분류한다.
 * {@code GptPerspectiveGenerationService} 와 같은 방식(RestClient + openai.* 설정)을 쓴다.
 */
@Slf4j
@Component
public class OpenAiEmotionClassifier implements EmotionClassifier {

    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 30000;

    private static final String TONE_VALUES =
            "NEUTRAL, ANGRY, SAD, EMBARRASSED, EMPHASIS, WHISPERING, SOFT_TONE, BREATHY, EXCITED";

    private static final String CLASSIFY_SYSTEM = """
            너는 철학 토론 대본의 대사에 감정 연출을 붙이는 도구다.
            각 줄에 대해:
            - tone: 그 줄의 대표 감성 톤 하나. 반드시 다음 중 하나: %s. 담담하면 NEUTRAL.
            - textWithEffects: 원문을 유지하되, 필요하면 오디오 효과 태그를 문장 안에 삽입한다.
              효과 태그 예: [sighing] [laughing] [chuckling] [clear throat] [break] [long-break].
              이미 있는 대괄호 태그는 지우지 마라. 톤 태그([angry] 등)는 textWithEffects 에 넣지 마라.
            반드시 JSON 만 출력: {"results":[{"index":0,"tone":"...","textWithEffects":"..."}]}
            """.formatted(TONE_VALUES);

    private static final String BIND_SYSTEM = """
            철학 토론에서 각 발화자가 A안과 B안 중 어느 쪽을 지지하는지 논지로 판단한다.
            반드시 JSON 만 출력: {"bindings":{"발화자이름":"A" 또는 "B"}}
            """;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.url}")
    private String openaiUrl;

    @Value("${openai.emotion.model:gpt-4o-mini}")
    private String model;

    @Override
    public Map<Integer, ScriptEmotion> classify(String battleTitle, List<ScriptLine> lines) {
        if (lines == null || lines.isEmpty()) {
            return Map.of();
        }
        try {
            List<Map<String, Object>> payload = new ArrayList<>();
            for (ScriptLine line : lines) {
                payload.add(Map.of(
                        "index", line.index(),
                        "speaker", line.speaker() == null ? "나레이터" : line.speaker(),
                        "text", line.text()));
            }
            String userPrompt = "배틀 제목: " + battleTitle + "\n대사 목록(JSON):\n"
                    + objectMapper.writeValueAsString(payload);

            JsonNode root = call(CLASSIFY_SYSTEM, userPrompt);
            Map<Integer, ScriptEmotion> out = new HashMap<>();
            for (JsonNode item : root.path("results")) {
                int index = item.path("index").asInt(-1);
                if (index < 0) {
                    continue;
                }
                Tone tone = Tone.fromNameOrNeutral(item.path("tone").asText(null));
                String text = item.path("textWithEffects").asText(null);
                out.put(index, new ScriptEmotion(tone, text));
            }
            return out;
        } catch (Exception e) {
            log.warn("[EmotionClassifier] 감정 분류 실패 - 전부 NEUTRAL 로 진행", e);
            return Map.of();
        }
    }

    @Override
    public Map<String, String> bindSpeakers(String battleTitle, String optionATitle, String optionBTitle,
                                            List<String> speakers) {
        if (speakers == null || speakers.isEmpty()) {
            return Map.of();
        }
        try {
            String userPrompt = "배틀 제목: " + battleTitle
                    + "\nA안: " + optionATitle
                    + "\nB안: " + optionBTitle
                    + "\n발화자: " + String.join(", ", speakers);

            JsonNode root = call(BIND_SYSTEM, userPrompt);
            Map<String, String> out = new HashMap<>();
            JsonNode bindings = root.path("bindings");
            bindings.fieldNames().forEachRemaining(name -> {
                String label = bindings.path(name).asText("").trim().toUpperCase();
                if (label.equals("A") || label.equals("B")) {
                    out.put(name, label);
                }
            });
            return out;
        } catch (Exception e) {
            log.warn("[EmotionClassifier] 발화자 바인딩 실패", e);
            return Map.of();
        }
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

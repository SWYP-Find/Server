package com.swyp.picke.domain.classroom.service.report;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.core.ParameterizedTypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * 리포트 생성용 OpenAI chat 호출. JSON 모드(response_format=json_object)로 받고 usage 토큰을 함께 돌려준다 (PRD §21 비용 기록).
 * 제공자 호출 한도 45초 (PRD §22). 기존 {@code GptPerspectiveGenerationService} 와 같은 설정 키를 쓴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClassReportAiClient {

    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 45_000;

    private final ObjectMapper objectMapper;

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.url}")
    private String openaiUrl;

    @Value("${openai.model}")
    private String model;

    /** 파싱된 JSON 본문과 토큰 사용량 */
    public record AiChatResult(Map<String, Object> json, String rawContent, Integer inputTokens, Integer outputTokens) {
    }

    public String getModel() {
        return model;
    }

    public AiChatResult chatJson(String systemPrompt, String userPrompt, int maxOutputTokens) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        RestClient restClient = RestClient.builder().requestFactory(factory).build();

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "response_format", Map.of("type", "json_object"),
                "temperature", 0.3,
                "max_tokens", maxOutputTokens
        );

        Map<String, Object> response = restClient.post()
                .uri(openaiUrl)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(requestBody)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {
                });

        if (response == null) {
            throw new IllegalStateException("empty response from provider");
        }
        List<Map<String, Object>> choices = ReportContent.mapList(response, "choices");
        if (choices.isEmpty()) {
            throw new IllegalStateException("no choices in provider response");
        }
        Map<String, Object> choice = choices.get(0);
        Map<String, Object> message = ReportContent.map(choice, "message");
        String content = ReportContent.str(message, "content");
        String finishReason = ReportContent.str(choice, "finish_reason");
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("empty content from provider");
        }
        if ("length".equals(finishReason)) {
            // 출력 한도에서 잘린 JSON 은 불완전하므로 성공으로 다루지 않는다 (A13)
            throw new ReportValidationException("OUTPUT_TRUNCATED");
        }

        Map<String, Object> usage = ReportContent.map(response, "usage");
        Integer inputTokens = usage == null ? null : toInt(usage.get("prompt_tokens"));
        Integer outputTokens = usage == null ? null : toInt(usage.get("completion_tokens"));

        Map<String, Object> json;
        try {
            json = objectMapper.readValue(content, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            throw new ReportValidationException("INVALID_JSON");
        }
        return new AiChatResult(json, content, inputTokens, outputTokens);
    }

    private static Integer toInt(Object value) {
        Long l = ReportContent.asLong(value);
        return l == null ? null : l.intValue();
    }
}

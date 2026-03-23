package com.swyp.app.domain.perspective.service;

import com.swyp.app.domain.perspective.enums.PerspectiveStatus;
import com.swyp.app.domain.perspective.repository.PerspectiveRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GptModerationService {

    // 프롬프트는 추후 결정
    private static final String SYSTEM_PROMPT =
            "당신은 콘텐츠 검수 AI입니다. 입력된 텍스트에 욕설, 혐오 발언, 폭력적 표현, 성적 표현, 특정인을 향한 공격적 내용이 포함되어 있는지 판단하세요. " +
            "문제가 있으면 'REJECT', 없으면 'APPROVE' 딱 한 단어만 응답하세요.";

    private static final int MAX_ATTEMPTS = 2;
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 10000;
    private static final int WAIT_TIMEOUT_MS = 2000;

    private final PerspectiveRepository perspectiveRepository;

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.url}")
    private String openaiUrl;

    @Value("${openai.model}")
    private String model;

    @Async
    public void moderate(Long perspectiveId, String content) {
        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                String result = callGpt(content);
                PerspectiveStatus newStatus = result.contains("APPROVE")
                        ? PerspectiveStatus.PUBLISHED
                        : PerspectiveStatus.REJECTED;

                perspectiveRepository.findById(perspectiveId).ifPresent(p -> {
                    if (p.getStatus() == PerspectiveStatus.PENDING) {
                        if (newStatus == PerspectiveStatus.PUBLISHED) p.publish();
                        else p.reject();
                        perspectiveRepository.save(p);
                    }
                });
                return;
            } catch (Exception e) {
                lastException = e;
                if (attempt < MAX_ATTEMPTS) {
                    try { Thread.sleep(WAIT_TIMEOUT_MS); } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        log.error("GPT 검수 최종 실패 (재시도 소진). perspectiveId={}", perspectiveId, lastException);
        perspectiveRepository.findById(perspectiveId).ifPresent(p -> {
            if (p.getStatus() == PerspectiveStatus.PENDING) {
                p.updateStatus(PerspectiveStatus.MODERATION_FAILED);
                perspectiveRepository.save(p);
            }
        });
    }

    private String callGpt(String content) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        RestClient restClient = RestClient.builder().requestFactory(factory).build();

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", content)
                ),
                "max_tokens", 10
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
        return ((String) message.get("content")).trim().toUpperCase();
    }
}

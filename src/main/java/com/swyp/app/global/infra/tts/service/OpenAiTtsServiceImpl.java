package com.swyp.app.global.infra.tts.service;

import com.swyp.app.domain.scenario.enums.SpeakerType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
// @Primary - 사용할 때 주석 삭제
@Service
public class OpenAiTtsServiceImpl implements TtsService {

    @Value("${openai.api-key}")
    private String openAiApiKey;

    @Value("${openai.tts.model:gpt-4o-mini-tts}")
    private String ttsModel;

    @Value("${openai.tts.url:https://api.openai.com/v1/audio/speech}")
    private String ttsUrl;

    @Override
    public File generateTtsWithSsml(String rawText, SpeakerType speakerType) throws Exception {
        // 1. 억지스러운 전처리 제거 (자연스러운 문장 부호 유지)
        String actingText = cleanTextForNaturalFlow(rawText);

        String voice = getOpenAiVoice(speakerType);
        double speed = getVoiceSpeed(speakerType);

        log.info("[TTS 호출] OpenAI 호출 (화자: {}, 속도: {}, 대사: '{}')", voice, speed, actingText);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiApiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", ttsModel);
        requestBody.put("input", actingText);
        requestBody.put("voice", voice);
        requestBody.put("response_format", "mp3");
        requestBody.put("speed", speed);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(ttsUrl, HttpMethod.POST, entity, byte[].class);
            File tempFile = File.createTempFile("tts_pro_" + UUID.randomUUID(), ".mp3");
            try (FileOutputStream out = new FileOutputStream(tempFile)) {
                out.write(response.getBody());
            }
            return tempFile;
        } catch (Exception e) {
            log.error("[TTS 호출 실패]", e);
            throw e;
        }
    }

    /**
     * 인위적인 쉼표 조작을 없애고, AI가 마침표(.)와 느낌표(!)를 보고
     * 스스로 억양을 잡게 합니다.
     */
    private String cleanTextForNaturalFlow(String rawText) {
        // SSML만 제거하고, 원래 문장의 쉼표와 마침표를 그대로 살립니다.
        // OpenAI는 마침표에서 톤을 낮추고, 느낌표에서 톤을 높이는 연기를 알아서 합니다.
        return rawText.replaceAll("<[^>]*>", "").replaceAll("\\s+", " ").trim();
    }

    private String getOpenAiVoice(SpeakerType type) {
        return switch (type) {
            case A -> "shimmer"; // 날카롭고 빠른 반응에 최적
            case B -> "fable";   // 단호한 반박
            case USER -> "alloy";
            case NARRATOR -> "onyx";
        };
    }

    /**
     * 박진감을 위해 속도를 1.15~1.2 수준으로 올립니다.
     * 1.2가 넘어가면 말이 뭉개질 수 있으니 여기가 마지노선입니다.
     */
    private double getVoiceSpeed(SpeakerType type) {
        return switch (type) {
            case NARRATOR -> 1.05; // 해설도 지루하지 않게
            case A, B -> 1.18;     // 🔥 대결 톤! 1.18~1.2 정도면 아주 긴박합니다.
            case USER -> 1.12;
        };
    }
}
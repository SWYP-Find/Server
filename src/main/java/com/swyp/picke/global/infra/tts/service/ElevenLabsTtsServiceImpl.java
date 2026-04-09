package com.swyp.picke.global.infra.tts.service;

import com.swyp.picke.domain.scenario.enums.SpeakerType;
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
public class ElevenLabsTtsServiceImpl implements TtsService {

    @Value("${elevenlabs.api-key}")
    private String apiKey;
    @Value("${elevenlabs.model}")
    private String ttsModel;
    @Value("${elevenlabs.voice-id.a}")
    private String voiceA;
    @Value("${elevenlabs.voice-id.b}")
    private String voiceB;
    @Value("${elevenlabs.voice-id.user}")
    private String voiceUser;
    @Value("${elevenlabs.voice-id.narrator}")
    private String voiceNarrator;

    private static final String ELEVENLABS_TTS_URL = "https://api.elevenlabs.io/v1/text-to-speech/";

    @Override
    public File generateTtsWithSsml(String rawText, SpeakerType speakerType) throws Exception {
        // 1. 순수 텍스트 추출 (OpenAI와 달리 쉼표 조작 없이 그대로 둠)
        String actingText = rawText.replaceAll("<[^>]*>", "").trim();
        String voiceId = getElevenLabsVoiceId(speakerType);

        log.info("[ElevenLabs] 🎭 연기 시작! 화자: {} ({}), 대사: '{}'",
                speakerType, voiceId, actingText.length() > 20 ? actingText.substring(0, 20) + "..." : actingText);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("xi-api-key", apiKey); // ElevenLabs 인증 헤더

        // 2. 박진감 넘치는 연기를 위한 파라미터 튜닝
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("text", actingText);
        requestBody.put("model_id", ttsModel);

        Map<String, Object> voiceSettings = new HashMap<>();
        voiceSettings.put("stability", 0.35);        // 낮을수록 감정이 풍부해짐 (0.3 ~ 0.4 추천)
        voiceSettings.put("similarity_boost", 0.80);  // 발음 선명도
        voiceSettings.put("style", 0.55);             // 연기력 부스트
        voiceSettings.put("use_speaker_boost", true); // 목소리를 더 또렷하게
        requestBody.put("voice_settings", voiceSettings);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    ELEVENLABS_TTS_URL + voiceId,
                    HttpMethod.POST,
                    entity,
                    byte[].class
            );

            if (response.getBody() == null) throw new RuntimeException("음성 데이터 생성 실패");

            File tempFile = File.createTempFile("tts_el_" + UUID.randomUUID(), ".mp3");
            try (FileOutputStream out = new FileOutputStream(tempFile)) {
                out.write(response.getBody());
            }
            return tempFile;

        } catch (Exception e) {
            log.error("[ElevenLabs 실패] API 키나 보이스 ID를 확인하세요.", e);
            throw e;
        }
    }

    private String getElevenLabsVoiceId(SpeakerType type) {
        return switch (type) {
            case A -> voiceA;
            case B -> voiceB;
            case USER -> voiceUser;
            case NARRATOR -> voiceNarrator;
        };
    }
}
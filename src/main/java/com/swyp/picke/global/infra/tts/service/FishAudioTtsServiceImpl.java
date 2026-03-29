package com.swyp.picke.global.infra.tts.service;

import com.swyp.picke.domain.scenario.enums.SpeakerType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Primary
@Service
public class FishAudioTtsServiceImpl implements TtsService {

    @Value("${fishaudio.api-key}")
    private String fishAudioApiKey;

    @Value("${fishaudio.tts.url:https://api.fish.audio/v1/tts}")
    private String ttsUrl;

    // application.yml에서 주입받는 화자별 Voice ID
    @Value("${fishaudio.voice-id.a}")
    private String voiceIdA;

    @Value("${fishaudio.voice-id.b}")
    private String voiceIdB;

    @Value("${fishaudio.voice-id.user}")
    private String voiceIdUser;

    @Value("${fishaudio.voice-id.narrator}")
    private String voiceIdNarrator;

    @Override
    public File generateTtsWithSsml(String rawText, SpeakerType speakerType) throws Exception {
        // 1. SSML 태그가 들어왔다면 제거하고 평문으로 변환 (Fish Audio는 평문에 강함)
        String actingText = cleanTextForNaturalFlow(rawText);

        // 2. Enum 타입에 따라 주입받은 Voice ID 매핑
        String referenceId = getFishAudioVoice(speakerType);

        log.info("[TTS 호출] Fish Audio 실행! (화자: {}, 대사: '{}')", speakerType.name(),
                actingText.length() > 15 ? actingText.substring(0, 15) + "..." : actingText);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(fishAudioApiKey);

        // 3. Fish Audio 요청
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("text", actingText);
        requestBody.put("reference_id", referenceId);
        requestBody.put("format", "mp3");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(ttsUrl, HttpMethod.POST, entity, byte[].class);

            File tempFile = File.createTempFile("tts_fish_" + UUID.randomUUID(), ".mp3");
            try (FileOutputStream out = new FileOutputStream(tempFile)) {
                if (response.getBody() != null) {
                    out.write(response.getBody());
                }
            }
            return tempFile;
        } catch (Exception e) {
            log.error("[TTS 호출 실패] Fish Audio API 통신 에러 - API 키와 모델 ID를 확인하세요.", e);
            throw e;
        }
    }

    /**
     * Fish Audio의 AI가 문맥과 문장 부호(!, ?, ...)만으로
     * 최적의 연기를 할 수 있도록 불필요한 태그를 지워줍니다.
     */
    private String cleanTextForNaturalFlow(String rawText) {
        return rawText.replaceAll("<[^>]*>", "").replaceAll("\\s+", " ").trim();
    }

    /**
     * 환경 변수로 주입받은 ID를 화자 타입에 맞게 반환합니다.
     */
    private String getFishAudioVoice(SpeakerType type) {
        return switch (type) {
            case A -> voiceIdA;           // 중후한 철학자 1
            case B -> voiceIdB;           // 중후한 철학자 2
            case USER -> voiceIdUser;     // 친근한 유저
            case NARRATOR -> voiceIdNarrator;   // 차분한 아나운서
        };
    }
}
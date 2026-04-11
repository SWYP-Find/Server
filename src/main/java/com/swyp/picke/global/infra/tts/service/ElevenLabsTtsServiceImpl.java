package com.swyp.picke.global.infra.tts.service;

import com.swyp.picke.domain.scenario.enums.SpeakerType;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
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
    public File generateTtsWithSsml(String rawText, SpeakerType speakerType, String customVoice) throws Exception {
        String actingText = rawText.replaceAll("<[^>]*>", "").trim();
        String voiceId = (customVoice != null && !customVoice.isBlank())
                ? customVoice.trim()
                : getElevenLabsVoiceId(speakerType);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("xi-api-key", apiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("text", actingText);
        requestBody.put("model_id", ttsModel);

        Map<String, Object> voiceSettings = new HashMap<>();
        voiceSettings.put("stability", 0.35);
        voiceSettings.put("similarity_boost", 0.80);
        voiceSettings.put("style", 0.55);
        voiceSettings.put("use_speaker_boost", true);
        requestBody.put("voice_settings", voiceSettings);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    ELEVENLABS_TTS_URL + voiceId,
                    HttpMethod.POST,
                    entity,
                    byte[].class
            );

            if (response.getBody() == null) {
                throw new CustomException(ErrorCode.TTS_GENERATION_FAILED);
            }

            File tempFile = File.createTempFile("tts_el_" + UUID.randomUUID(), ".mp3");
            try (FileOutputStream out = new FileOutputStream(tempFile)) {
                out.write(response.getBody());
            }
            return tempFile;
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("[ElevenLabs] TTS 생성 실패", e);
            throw new CustomException(ErrorCode.TTS_GENERATION_FAILED);
        }
    }

    private String getElevenLabsVoiceId(SpeakerType type) {
        String voiceId = switch (type) {
            case A -> voiceA;
            case B -> voiceB;
            case USER -> voiceUser;
            case NARRATOR -> voiceNarrator;
        };
        if (voiceId == null || voiceId.isBlank()) {
            throw new CustomException(ErrorCode.TTS_INVALID_VOICE_ID);
        }
        return voiceId;
    }
}

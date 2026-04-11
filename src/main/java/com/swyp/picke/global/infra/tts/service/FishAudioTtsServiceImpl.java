package com.swyp.picke.global.infra.tts.service;

import com.swyp.picke.domain.scenario.enums.SpeakerType;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
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
@Primary
@Service
public class FishAudioTtsServiceImpl implements TtsService {

    @Value("${fishaudio.api-key}")
    private String fishAudioApiKey;

    @Value("${fishaudio.tts.url:https://api.fish.audio/v1/tts}")
    private String ttsUrl;

    @Override
    public File generateTtsWithSsml(String rawText, SpeakerType speakerType, String customVoice) throws Exception {
        String actingText = cleanTextForNaturalFlow(rawText);
        String referenceId = resolveVoiceId(speakerType, customVoice);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(fishAudioApiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("text", actingText);
        requestBody.put("reference_id", referenceId);
        requestBody.put("format", "mp3");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(ttsUrl, HttpMethod.POST, entity, byte[].class);
            byte[] audioBytes = response.getBody();
            if (audioBytes == null || audioBytes.length == 0) {
                throw new CustomException(ErrorCode.TTS_GENERATION_FAILED);
            }

            File tempFile = File.createTempFile("tts_fish_" + UUID.randomUUID(), ".mp3");
            try (FileOutputStream out = new FileOutputStream(tempFile)) {
                out.write(audioBytes);
            }
            return tempFile;
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("[TTS 호출 실패] Fish Audio API 통신 에러 - API 키와 모델 ID를 확인하세요.", e);
            throw new CustomException(ErrorCode.TTS_GENERATION_FAILED);
        }
    }

    private String cleanTextForNaturalFlow(String rawText) {
        return rawText.replaceAll("<[^>]*>", "").replaceAll("\\s+", " ").trim();
    }

    private String resolveVoiceId(SpeakerType type, String customVoice) {
        if (customVoice == null || customVoice.isBlank()) {
            throw new CustomException(ErrorCode.TTS_INVALID_VOICE_ID);
        }
        return customVoice.trim();
    }
}

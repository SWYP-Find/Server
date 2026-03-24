package com.swyp.app.global.infra.tts.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.texttospeech.v1.*;
import com.google.protobuf.ByteString;
import com.swyp.app.domain.scenario.enums.SpeakerType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.UUID;

@Slf4j
// @Primary - 사용할 때 주석 삭제
@Service
public class GoogleCloudTtsServiceImpl implements TtsService {

    @Value("${spring.cloud.gcp.credentials.location}")
    private String credentialsLocation;

    @Override
    public File generateTtsWithSsml(String rawText, SpeakerType speakerType) throws Exception {
        // SSML 태그가 없으면 자동으로 씌워줍니다.
        String ssmlInput = rawText.trim().startsWith("<speak>") ? rawText : "<speak>" + rawText + "</speak>";

        try (FileInputStream credentialsStream = new FileInputStream(credentialsLocation)) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream);
            TextToSpeechSettings settings = TextToSpeechSettings.newBuilder()
                    .setCredentialsProvider(() -> credentials)
                    .build();

            try (TextToSpeechClient textToSpeechClient = TextToSpeechClient.create(settings)) {
                SynthesisInput input = SynthesisInput.newBuilder().setSsml(ssmlInput).build();
                VoiceSelectionParams voice = buildVoiceSelection(speakerType);
                AudioConfig audioConfig = AudioConfig.newBuilder().setAudioEncoding(AudioEncoding.MP3).build();

                // 실제 구글 API가 호출될 때만 찍히는 로그
                String logText = rawText.length() > 15 ? rawText.substring(0, 15) + "..." : rawText;
                log.info("[TTS 호출] 💳 구글 API 실제 요청 발생! (화자: {}, 대사: '{}')", speakerType.name(), logText);

                SynthesizeSpeechResponse response = textToSpeechClient.synthesizeSpeech(input, voice, audioConfig);
                ByteString audioContents = response.getAudioContent();

                File tempFile = File.createTempFile("tts_" + UUID.randomUUID(), ".mp3");
                try (FileOutputStream out = new FileOutputStream(tempFile)) {
                    out.write(audioContents.toByteArray());
                }
                return tempFile;
            }
        } catch (Exception e) {
            log.error("[TTS 호출 실패] GCP 키 파일 확인 필요: {}", credentialsLocation, e);
            throw e;
        }
    }

    private VoiceSelectionParams buildVoiceSelection(SpeakerType type) {
        String voiceName = switch (type) {
            case A -> "ko-KR-Wavenet-C";
            case B -> "ko-KR-Wavenet-D";
            case USER -> "ko-KR-Wavenet-B";
            case NARRATOR -> "ko-KR-Wavenet-A";
        };
        return VoiceSelectionParams.newBuilder()
                .setLanguageCode("ko-KR")
                .setName(voiceName)
                .build();
    }
}
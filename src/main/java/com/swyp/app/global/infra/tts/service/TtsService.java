package com.swyp.app.global.infra.tts.service;

import com.swyp.app.domain.scenario.enums.SpeakerType;
import java.io.File;

/**
 * 외부 TTS API(구글 클라우드 등)를 호출하는 기능을 정의하는 인터페이스
 */
public interface TtsService {
    File generateTtsWithSsml(String text, SpeakerType speakerType) throws Exception;
}
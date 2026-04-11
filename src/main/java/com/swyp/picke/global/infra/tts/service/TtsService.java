package com.swyp.picke.global.infra.tts.service;

import com.swyp.picke.domain.scenario.enums.SpeakerType;
import java.io.File;

public interface TtsService {
    File generateTtsWithSsml(String text, SpeakerType speakerType, String customVoice) throws Exception;
}
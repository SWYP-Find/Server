package com.swyp.picke.global.infra.tts.service;

import com.swyp.picke.domain.scenario.enums.SpeakerType;
import com.swyp.picke.domain.scenario.enums.Tone;
import java.io.File;

public interface TtsService {
    File generateTtsWithSsml(String text, SpeakerType speakerType, String customVoice, Tone tone) throws Exception;
}

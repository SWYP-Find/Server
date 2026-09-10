package com.swyp.picke.domain.scenario.dto.request;

import com.swyp.picke.domain.scenario.enums.SpeakerType;
import com.swyp.picke.domain.scenario.enums.Tone;

public record ScriptRequest(
        String speakerName,
        SpeakerType speakerType,
        String text,
        Tone tone
) {}

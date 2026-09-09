package com.swyp.picke.domain.admin.dto.scenario.request;

import com.swyp.picke.domain.scenario.enums.SpeakerType;
import com.swyp.picke.domain.scenario.enums.Tone;

public record AdminScenarioScriptRequest(
        String speakerName,
        SpeakerType speakerType,
        String text,
        Tone tone
) {}

package com.swyp.picke.domain.admin.dto.scenario.response;

import com.swyp.picke.domain.scenario.enums.SpeakerType;
import com.swyp.picke.domain.scenario.enums.Tone;
import lombok.Builder;

@Builder
public record AdminScenarioScriptResponse(
        Long scriptId,
        Integer startTimeMs,
        SpeakerType speakerType,
        String speakerName,
        String text,
        Tone tone
) {}

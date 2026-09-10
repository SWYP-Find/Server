package com.swyp.picke.domain.scenario.dto.response;

import com.swyp.picke.domain.scenario.enums.SpeakerType;
import com.swyp.picke.domain.scenario.enums.Tone;
import lombok.Builder;

@Builder
public record ScriptResponse(
        Long scriptId,
        Integer startTimeMs,
        SpeakerType speakerType,
        String speakerName,
        String text,
        Tone tone
) {}

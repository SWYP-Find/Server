package com.swyp.app.domain.scenario.dto.response;

import com.swyp.app.domain.scenario.enums.SpeakerType;
import lombok.Builder;
import java.util.UUID;

@Builder
public record ScriptResponse(
        UUID scriptId,
        Integer startTimeMs,
        SpeakerType speakerType,
        String speakerName,
        String text
) {}
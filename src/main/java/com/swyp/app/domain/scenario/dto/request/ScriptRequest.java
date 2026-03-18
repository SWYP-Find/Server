package com.swyp.app.domain.scenario.dto.request;

import com.swyp.app.domain.scenario.enums.SpeakerType;

public record ScriptRequest(
        String speakerName,
        SpeakerType speakerType,
        String text
) {}
package com.swyp.picke.domain.scenario.dto.request;

import com.swyp.picke.domain.scenario.enums.SpeakerType;

public record ScriptRequest(
        String speakerName,
        SpeakerType speakerType,
        String text
) {}
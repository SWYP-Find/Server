package com.swyp.picke.domain.admin.dto.scenario.request;

import com.swyp.picke.domain.scenario.enums.SpeakerType;

public record AdminScenarioScriptRequest(
        String speakerName,
        SpeakerType speakerType,
        String text
) {}

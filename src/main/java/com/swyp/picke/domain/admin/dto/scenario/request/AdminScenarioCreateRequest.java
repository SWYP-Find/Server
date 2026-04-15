package com.swyp.picke.domain.admin.dto.scenario.request;

import com.swyp.picke.domain.scenario.enums.ScenarioStatus;
import com.swyp.picke.domain.scenario.enums.SpeakerType;

import java.util.List;
import java.util.Map;

public record AdminScenarioCreateRequest(
        Long battleId,
        Boolean isInteractive,
        ScenarioStatus status,
        List<AdminScenarioNodeRequest> nodes,
        Map<SpeakerType, String> voiceSettings
) {}

package com.swyp.picke.domain.scenario.dto.request;

import com.swyp.picke.domain.scenario.enums.ScenarioStatus;
import com.swyp.picke.domain.scenario.enums.SpeakerType;
import java.util.List;
import java.util.Map;

public record ScenarioCreateRequest(
        Long battleId,
        Boolean isInteractive,
        ScenarioStatus status,
        List<NodeRequest> nodes,
        Map<SpeakerType, String> voiceSettings
) {}
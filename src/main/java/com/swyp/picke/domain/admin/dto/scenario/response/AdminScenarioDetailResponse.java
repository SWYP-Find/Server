package com.swyp.picke.domain.admin.dto.scenario.response;

import com.swyp.picke.domain.scenario.enums.SpeakerType;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record AdminScenarioDetailResponse(
        Long scenarioId,
        Long battleId,
        String title,
        Boolean isInteractive,
        List<AdminScenarioNodeResponse> nodes,
        Map<SpeakerType, String> voiceSettings
) {}


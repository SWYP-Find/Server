package com.swyp.picke.domain.scenario.dto.response;

import com.swyp.picke.domain.scenario.enums.ScenarioStatus;

public record AdminScenarioResponse(
        Long scenarioId,
        ScenarioStatus status,
        String message
) {}

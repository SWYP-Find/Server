package com.swyp.app.domain.scenario.dto.response;

import com.swyp.app.domain.scenario.enums.ScenarioStatus;

public record AdminScenarioResponse(
        Long scenarioId,
        ScenarioStatus status,
        String message
) {}

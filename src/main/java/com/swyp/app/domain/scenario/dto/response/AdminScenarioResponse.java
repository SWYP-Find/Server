package com.swyp.app.domain.scenario.dto.response;

import com.swyp.app.domain.scenario.enums.ScenarioStatus;

import java.util.UUID;

public record AdminScenarioResponse(
        UUID scenarioId,
        ScenarioStatus status,
        String message
) {}

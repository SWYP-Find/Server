package com.swyp.app.domain.scenario.dto.request;

import com.swyp.app.domain.scenario.enums.ScenarioStatus;

public record ScenarioStatusUpdateRequest(
        ScenarioStatus status
) {}
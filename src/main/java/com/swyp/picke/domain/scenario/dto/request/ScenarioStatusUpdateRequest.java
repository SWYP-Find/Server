package com.swyp.picke.domain.scenario.dto.request;

import com.swyp.picke.domain.scenario.enums.ScenarioStatus;

public record ScenarioStatusUpdateRequest(
        ScenarioStatus status
) {}
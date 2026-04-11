package com.swyp.picke.domain.admin.dto.scenario.request;

import com.swyp.picke.domain.scenario.enums.ScenarioStatus;

public record AdminScenarioStatusUpdateRequest(
        ScenarioStatus status
) {}

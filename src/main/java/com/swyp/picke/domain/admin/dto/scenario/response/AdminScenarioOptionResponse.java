package com.swyp.picke.domain.admin.dto.scenario.response;

import lombok.Builder;

@Builder
public record AdminScenarioOptionResponse(
        String label,
        Long nextNodeId
) {}

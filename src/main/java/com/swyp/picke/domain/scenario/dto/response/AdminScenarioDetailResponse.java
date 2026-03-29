package com.swyp.picke.domain.scenario.dto.response;

import lombok.Builder;
import java.util.List;

@Builder
public record AdminScenarioDetailResponse(
        Long scenarioId,
        Long battleId,
        Boolean isInteractive,
        List<NodeResponse> nodes
) {}
package com.swyp.app.domain.scenario.dto.request;

import java.util.List;

public record ScenarioCreateRequest(
        Long battleId,
        Boolean isInteractive,
        List<NodeRequest> nodes
) {}
package com.swyp.app.domain.scenario.dto.request;

import java.util.List;
import java.util.UUID;

public record ScenarioCreateRequest(
        UUID battleId,
        Boolean isInteractive,
        List<NodeRequest> nodes
) {}
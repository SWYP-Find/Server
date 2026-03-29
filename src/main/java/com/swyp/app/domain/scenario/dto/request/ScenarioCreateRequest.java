package com.swyp.app.domain.scenario.dto.request;

import com.swyp.app.domain.scenario.enums.ScenarioStatus;
import java.util.List;

public record ScenarioCreateRequest(
        Long battleId,
        Boolean isInteractive,
        ScenarioStatus status,
        List<NodeRequest> nodes
) {}
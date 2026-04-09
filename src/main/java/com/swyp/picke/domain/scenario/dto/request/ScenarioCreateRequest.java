package com.swyp.picke.domain.scenario.dto.request;

import com.swyp.picke.domain.scenario.enums.ScenarioStatus;
import java.util.List;

public record ScenarioCreateRequest(
        Long battleId,
        Boolean isInteractive,
        ScenarioStatus status,
        List<NodeRequest> nodes
) {}
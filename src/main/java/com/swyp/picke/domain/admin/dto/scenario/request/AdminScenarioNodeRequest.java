package com.swyp.picke.domain.admin.dto.scenario.request;

import java.util.List;

public record AdminScenarioNodeRequest(
        String nodeName,
        Boolean isStartNode,
        String autoNextNode,
        List<AdminScenarioScriptRequest> scripts,
        List<AdminScenarioOptionRequest> interactiveOptions
) {}

package com.swyp.picke.domain.admin.dto.scenario.response;

import lombok.Builder;

import java.util.List;

@Builder
public record AdminScenarioNodeResponse(
        Long nodeId,
        String nodeName,
        Boolean isStartNode,
        Integer audioDuration,
        Long autoNextNodeId,
        String autoNextNode,
        List<AdminScenarioScriptResponse> scripts,
        List<AdminScenarioOptionResponse> interactiveOptions
) {}

package com.swyp.picke.domain.scenario.dto.response;

import lombok.Builder;
import java.util.List;

@Builder
public record NodeResponse(
        Long nodeId,
        String nodeName,
        Integer audioDuration,
        Long autoNextNodeId,
        List<ScriptResponse> scripts,
        List<OptionResponse> interactiveOptions
) {}
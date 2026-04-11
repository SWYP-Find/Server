package com.swyp.picke.domain.scenario.dto.request;

import java.util.List;

public record NodeRequest(
        String nodeName,
        Boolean isStartNode,
        String autoNextNode,
        List<ScriptRequest> scripts,
        List<OptionRequest> interactiveOptions
) {}
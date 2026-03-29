package com.swyp.picke.domain.scenario.dto.request;

import java.util.List;

public record NodeRequest(
        String nodeName,
        Boolean isStartNode,
        String autoNextNode, // 자동 넘김 노드 이름 추가
        List<ScriptRequest> scripts,
        List<OptionRequest> interactiveOptions
) {}
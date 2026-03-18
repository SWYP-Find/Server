package com.swyp.app.domain.scenario.dto.response;

import lombok.Builder;
import java.util.List;
import java.util.UUID;

@Builder
public record NodeResponse(
        UUID nodeId,
        String nodeName,
        Integer audioDuration, // 프론트엔드 재생 시간 표시에 활용
        UUID autoNextNodeId,
        List<ScriptResponse> scripts,
        List<OptionResponse> interactiveOptions
) {}
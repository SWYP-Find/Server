package com.swyp.picke.domain.scenario.dto.response;

import lombok.Builder;
import java.util.List;

@Builder
public record NodeResponse(
        Long nodeId,
        String nodeName,
        Integer audioDuration, // 프론트엔드 재생 시간 표시에 활용
        Long autoNextNodeId,
        List<ScriptResponse> scripts,
        List<OptionResponse> interactiveOptions
) {}
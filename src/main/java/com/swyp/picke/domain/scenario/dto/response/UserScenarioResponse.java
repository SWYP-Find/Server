package com.swyp.picke.domain.scenario.dto.response;

import com.swyp.picke.domain.scenario.enums.AudioPathType;
import lombok.Builder;
import java.util.List;
import java.util.Map;

@Builder
public record UserScenarioResponse(
        Long battleId,
        Boolean isInteractive,
        Long startNodeId,                 // 프론트가 텍스트 시작점을 잡을 수 있게 전달
        AudioPathType recommendedPathKey, // 사전 투표 기반 추천 오디오 키 (예: PATH_A)
        Map<AudioPathType, String> audios, // 통합 오디오 파일 맵
        List<NodeResponse> nodes
) {}
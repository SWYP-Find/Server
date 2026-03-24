package com.swyp.app.domain.scenario.converter;

import com.swyp.app.domain.scenario.dto.response.NodeResponse;
import com.swyp.app.domain.scenario.dto.response.OptionResponse;
import com.swyp.app.domain.scenario.dto.response.ScriptResponse;
import com.swyp.app.domain.scenario.dto.response.UserScenarioResponse;
import com.swyp.app.domain.scenario.entity.InteractiveOption;
import com.swyp.app.domain.scenario.entity.Scenario;
import com.swyp.app.domain.scenario.entity.ScenarioNode;
import com.swyp.app.domain.scenario.entity.Script;
import com.swyp.app.domain.scenario.enums.AudioPathType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ScenarioConverter {

    /**
     * Scenario 엔티티를 프론트엔드 전달용 DTO로 변환합니다.
     * @param scenario DB에서 조회된 시나리오 엔티티
     * @param recommendedPathKey 사전 투표 결과에 따른 추천 오디오 키 (COMMON, PATH_A, PATH_B)
     */
    public UserScenarioResponse toUserResponse(Scenario scenario, AudioPathType recommendedPathKey) {

        // 1. 시작 노드 ID 찾기
        Long startNodeId = scenario.getNodes().stream()
                .filter(node -> Boolean.TRUE.equals(node.getIsStartNode()))
                .map(ScenarioNode::getId)
                .findFirst()
                .orElse(null);

        // 2. 하위 노드 리스트 변환
        List<NodeResponse> nodeResponses = scenario.getNodes().stream()
                .map(this::toNodeResponse)
                .collect(Collectors.toList());

        // 3. 최종 응답 빌드
        return UserScenarioResponse.builder()
                .battleId(scenario.getBattle().getId())
                .isInteractive(scenario.getIsInteractive())
                .startNodeId(startNodeId)
                .recommendedPathKey(recommendedPathKey)
                .audios(scenario.getAudios())
                .nodes(nodeResponses)
                .build();
    }

    private NodeResponse toNodeResponse(ScenarioNode node) {
        return NodeResponse.builder()
                .nodeId(node.getId())
                .nodeName(node.getNodeName())
                .audioDuration(node.getAudioDuration()) // 노드별 재생 시간 전달
                .autoNextNodeId(node.getAutoNextNodeId())
                .scripts(node.getScripts().stream()
                        .map(this::toScriptResponse)
                        .collect(Collectors.toList()))
                .interactiveOptions(node.getOptions().stream()
                        .map(this::toOptionResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    private ScriptResponse toScriptResponse(Script script) {

        // 1. 정규식 사용
        // \\[.*?\\] : 대괄호로 묶인 모든 문자열 찾기 (예: [pause], [angry])
        String cleanText = script.getText()
                .replaceAll("\\[.*?\\]", "") // 태그 제거
                .replaceAll("\\s+", " ")     // 태그가 빠진 자리에 남은 중복 공백들을 하나로 합침
                .trim();                     // 양끝 공백 제거

        return ScriptResponse.builder()
                .scriptId(script.getId())
                .startTimeMs(script.getStartTimeMs())
                .speakerType(script.getSpeakerType())
                .speakerName(script.getSpeakerName())
                // 2. 정제된 cleanText
                .text(cleanText)
                .build();
    }

    private OptionResponse toOptionResponse(InteractiveOption option) {
        return OptionResponse.builder()
                .label(option.getLabel())
                .nextNodeId(option.getNextNodeId())
                .build();
    }
}
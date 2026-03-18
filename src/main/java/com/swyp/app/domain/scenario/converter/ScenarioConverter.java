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
import java.util.UUID;
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
        UUID startNodeId = scenario.getNodes().stream()
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
        return ScriptResponse.builder()
                .scriptId(script.getId())
                .startTimeMs(script.getStartTimeMs()) // 자막 띄우는 핵심 싱크 타이밍
                .speakerType(script.getSpeakerType())
                .speakerName(script.getSpeakerName())
                .text(script.getText())
                .build();
    }

    private OptionResponse toOptionResponse(InteractiveOption option) {
        return OptionResponse.builder()
                .label(option.getLabel())
                .nextNodeId(option.getNextNodeId())
                .build();
    }
}
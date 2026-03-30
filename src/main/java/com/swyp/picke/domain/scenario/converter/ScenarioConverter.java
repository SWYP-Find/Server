package com.swyp.picke.domain.scenario.converter;

import com.swyp.picke.domain.scenario.dto.response.*;
import com.swyp.picke.domain.scenario.entity.InteractiveOption;
import com.swyp.picke.domain.scenario.entity.Scenario;
import com.swyp.picke.domain.scenario.entity.ScenarioNode;
import com.swyp.picke.domain.scenario.entity.Script;
import com.swyp.picke.domain.scenario.enums.AudioPathType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ScenarioConverter {

    @Value("${picke_base_url}")
    private String baseUrl;

    /**
     * [유저용] Scenario 엔티티를 프론트엔드 전달용 DTO로 변환합니다.
     */
    public UserScenarioResponse toUserResponse(Scenario scenario, AudioPathType recommendedPathKey) {
        Long startNodeId = scenario.getNodes().stream()
                .filter(node -> Boolean.TRUE.equals(node.getIsStartNode()))
                .map(ScenarioNode::getId)
                .findFirst()
                .orElse(null);

        List<NodeResponse> nodeResponses = scenario.getNodes().stream()
                .map(this::toUserNodeResponse)
                .collect(Collectors.toList());

        // 💡 에러 완벽 수정: Key 타입을 AudioPathType으로 맞추고 그대로 put
        Map<AudioPathType, String> fullUrlAudios = new HashMap<>();
        if (scenario.getAudios() != null) {
            scenario.getAudios().forEach((key, path) -> {
                String fullPath = (path != null && !path.startsWith("http")) ? baseUrl + path : path;
                fullUrlAudios.put(key, fullPath);
            });
        }

        return UserScenarioResponse.builder()
                .battleId(scenario.getBattle().getId())
                .isInteractive(scenario.getIsInteractive())
                .startNodeId(startNodeId)
                .recommendedPathKey(recommendedPathKey)
                .audios(fullUrlAudios) // 병합된 오디오에만 Base URL 적용!
                .nodes(nodeResponses)
                .build();
    }

    /**
     * [관리자용] 시나리오 상세 변환 메서드
     */
    public AdminScenarioDetailResponse toAdminDetailResponse(Scenario scenario) {
        return AdminScenarioDetailResponse.builder()
                .scenarioId(scenario.getId())
                .battleId(scenario.getBattle().getId())
                .isInteractive(scenario.getIsInteractive())
                .nodes(scenario.getNodes().stream()
                        .map(this::toAdminNodeResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    // ==========================================
    // 🟢 유저용 변환 로직 (오디오 URL 제거, 순수 데이터만)
    // ==========================================
    private NodeResponse toUserNodeResponse(ScenarioNode node) {
        return NodeResponse.builder()
                .nodeId(node.getId())
                .nodeName(node.getNodeName())
                .audioDuration(node.getAudioDuration())
                .autoNextNodeId(node.getAutoNextNodeId())
                .scripts(node.getScripts().stream()
                        .map(this::toUserScriptResponse)
                        .collect(Collectors.toList()))
                .interactiveOptions(node.getOptions().stream()
                        .map(this::toOptionResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    private ScriptResponse toUserScriptResponse(Script script) {
        String cleanText = script.getText()
                .replaceAll("\\[.*?\\]", "")
                .replaceAll("\\s+", " ")
                .trim();

        return ScriptResponse.builder()
                .scriptId(script.getId())
                .startTimeMs(script.getStartTimeMs())
                .speakerType(script.getSpeakerType())
                .speakerName(script.getSpeakerName())
                .text(cleanText)
                .build();
    }

    // ==========================================
    // 🔵 관리자용 변환 로직 (오디오 URL 제거)
    // ==========================================
    private NodeResponse toAdminNodeResponse(ScenarioNode node) {
        return NodeResponse.builder()
                .nodeId(node.getId())
                .nodeName(node.getNodeName())
                .audioDuration(node.getAudioDuration())
                .autoNextNodeId(node.getAutoNextNodeId())
                .scripts(node.getScripts().stream()
                        .map(this::toAdminScriptResponse)
                        .collect(Collectors.toList()))
                .interactiveOptions(node.getOptions().stream()
                        .map(this::toOptionResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    private ScriptResponse toAdminScriptResponse(Script script) {
        return ScriptResponse.builder()
                .scriptId(script.getId())
                .startTimeMs(script.getStartTimeMs())
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
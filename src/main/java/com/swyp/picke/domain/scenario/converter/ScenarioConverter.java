package com.swyp.picke.domain.scenario.converter;

import com.swyp.picke.domain.scenario.dto.response.*;
import com.swyp.picke.domain.scenario.entity.InteractiveOption;
import com.swyp.picke.domain.scenario.entity.Scenario;
import com.swyp.picke.domain.scenario.entity.ScenarioNode;
import com.swyp.picke.domain.scenario.entity.Script;
import com.swyp.picke.domain.scenario.enums.AudioPathType;
import com.swyp.picke.global.infra.s3.util.ResourceUrlProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ScenarioConverter {

    private final ResourceUrlProvider resourceUrlProvider;
    private static final String BASE_SHARE_URL = "https://pique.app/battles/";

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

        Map<AudioPathType, String> fullUrlAudios = new HashMap<>();
        if (scenario.getAudios() != null) {
            scenario.getAudios().forEach((audioPathType, fileName) -> {
                String publicAudioUrl = resourceUrlProvider.getAudioUrl(scenario.getId(), fileName);
                fullUrlAudios.put(audioPathType, publicAudioUrl);
            });
        }

        return UserScenarioResponse.builder()
                .battleId(scenario.getBattle().getId())
                .isInteractive(scenario.getIsInteractive())
                .startNodeId(startNodeId)
                .recommendedPathKey(recommendedPathKey)
                .audios(fullUrlAudios)
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

    // 유저용 변환 로직
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

    // 관리자용 변환 로직
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
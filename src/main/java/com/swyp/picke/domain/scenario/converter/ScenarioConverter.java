package com.swyp.picke.domain.scenario.converter;

import com.swyp.picke.domain.admin.dto.scenario.response.AdminScenarioDetailResponse;
import com.swyp.picke.domain.admin.dto.scenario.response.AdminScenarioNodeResponse;
import com.swyp.picke.domain.admin.dto.scenario.response.AdminScenarioOptionResponse;
import com.swyp.picke.domain.admin.dto.scenario.response.AdminScenarioScriptResponse;
import com.swyp.picke.domain.scenario.dto.response.*;
import com.swyp.picke.domain.scenario.entity.InteractiveOption;
import com.swyp.picke.domain.scenario.entity.Scenario;
import com.swyp.picke.domain.scenario.entity.ScenarioNode;
import com.swyp.picke.domain.scenario.entity.Script;
import com.swyp.picke.domain.scenario.enums.AudioPathType;
import com.swyp.picke.global.infra.s3.util.ResourceUrlProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ScenarioConverter {

    private final ResourceUrlProvider resourceUrlProvider;

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
                .title(scenario.getBattle().getTitle())
                .isInteractive(scenario.getIsInteractive())
                .startNodeId(startNodeId)
                .recommendedPathKey(recommendedPathKey)
                .audios(fullUrlAudios)
                .nodes(nodeResponses)
                .build();
    }

        public AdminScenarioDetailResponse toAdminDetailResponse(Scenario scenario) {
        return AdminScenarioDetailResponse.builder()
                .scenarioId(scenario.getId())
                .battleId(scenario.getBattle().getId())
                .title(scenario.getBattle().getTitle())
                .isInteractive(scenario.getIsInteractive())
                .voiceSettings(new HashMap<>(scenario.getVoiceSettings()))
                .nodes(scenario.getNodes().stream()
                        .map(this::toAdminNodeResponse)
                        .collect(Collectors.toList()))
                .build();
    }

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
                        .map(this::toUserOptionResponse)
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

    private AdminScenarioNodeResponse toAdminNodeResponse(ScenarioNode node) {
        return AdminScenarioNodeResponse.builder()
                .nodeId(node.getId())
                .nodeName(node.getNodeName())
                .audioDuration(node.getAudioDuration())
                .autoNextNodeId(node.getAutoNextNodeId())
                .scripts(node.getScripts().stream()
                        .map(this::toAdminScriptResponse)
                        .collect(Collectors.toList()))
                .interactiveOptions(node.getOptions().stream()
                        .map(this::toAdminOptionResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    private AdminScenarioScriptResponse toAdminScriptResponse(Script script) {
        return AdminScenarioScriptResponse.builder()
                .scriptId(script.getId())
                .startTimeMs(script.getStartTimeMs())
                .speakerType(script.getSpeakerType())
                .speakerName(script.getSpeakerName())
                .text(script.getText())
                .build();
    }

    private OptionResponse toUserOptionResponse(InteractiveOption option) {
        return OptionResponse.builder()
                .label(option.getLabel())
                .nextNodeId(option.getNextNodeId())
                .build();
    }

    private AdminScenarioOptionResponse toAdminOptionResponse(InteractiveOption option) {
        return AdminScenarioOptionResponse.builder()
                .label(option.getLabel())
                .nextNodeId(option.getNextNodeId())
                .build();
    }
}
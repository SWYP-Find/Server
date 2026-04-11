package com.swyp.picke.domain.admin.service;

import com.swyp.picke.domain.admin.dto.scenario.request.AdminScenarioCreateRequest;
import com.swyp.picke.domain.admin.dto.scenario.request.AdminScenarioNodeRequest;
import com.swyp.picke.domain.admin.dto.scenario.request.AdminScenarioOptionRequest;
import com.swyp.picke.domain.admin.dto.scenario.request.AdminScenarioScriptRequest;
import com.swyp.picke.domain.admin.dto.scenario.response.AdminDeleteResponse;
import com.swyp.picke.domain.admin.dto.scenario.response.AdminScenarioCreateResponse;
import com.swyp.picke.domain.admin.dto.scenario.response.AdminScenarioDetailResponse;
import com.swyp.picke.domain.admin.dto.scenario.response.AdminScenarioResponse;
import com.swyp.picke.domain.scenario.dto.request.NodeRequest;
import com.swyp.picke.domain.scenario.dto.request.OptionRequest;
import com.swyp.picke.domain.scenario.dto.request.ScenarioCreateRequest;
import com.swyp.picke.domain.scenario.dto.request.ScriptRequest;
import com.swyp.picke.domain.scenario.enums.ScenarioStatus;
import com.swyp.picke.domain.scenario.service.ScenarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminScenarioService {

    private final ScenarioService scenarioService;

    public AdminScenarioDetailResponse getScenarioForAdmin(Long battleId) {
        return toAdminScenarioDetailResponse(scenarioService.getScenarioForAdmin(battleId));
    }

    public AdminScenarioCreateResponse createScenario(AdminScenarioCreateRequest request) {
        Long scenarioId = scenarioService.createScenario(toScenarioCreateRequest(request));
        return new AdminScenarioCreateResponse(scenarioId, request.status());
    }

    public void updateScenarioContent(Long scenarioId, AdminScenarioCreateRequest request) {
        scenarioService.updateScenarioContent(scenarioId, toScenarioCreateRequest(request));
    }

    public AdminScenarioResponse updateScenarioStatus(Long scenarioId, ScenarioStatus status) {
        return toAdminScenarioResponse(scenarioService.updateScenarioStatus(scenarioId, status));
    }

    public AdminDeleteResponse deleteScenario(Long scenarioId) {
        return toAdminDeleteResponse(scenarioService.deleteScenario(scenarioId));
    }

    private ScenarioCreateRequest toScenarioCreateRequest(AdminScenarioCreateRequest request) {
        return new ScenarioCreateRequest(
                request.battleId(),
                request.isInteractive(),
                request.status(),
                toNodeRequests(request.nodes())
        );
    }

    private List<NodeRequest> toNodeRequests(List<AdminScenarioNodeRequest> nodeRequests) {
        if (nodeRequests == null) {
            return List.of();
        }
        return nodeRequests.stream()
                .map(this::toNodeRequest)
                .toList();
    }

    private NodeRequest toNodeRequest(AdminScenarioNodeRequest nodeRequest) {
        return new NodeRequest(
                nodeRequest.nodeName(),
                nodeRequest.isStartNode(),
                nodeRequest.autoNextNode(),
                toScriptRequests(nodeRequest.scripts()),
                toOptionRequests(nodeRequest.interactiveOptions())
        );
    }

    private List<ScriptRequest> toScriptRequests(List<AdminScenarioScriptRequest> scriptRequests) {
        if (scriptRequests == null) {
            return null;
        }
        return scriptRequests.stream()
                .map(scriptRequest -> new ScriptRequest(
                        scriptRequest.speakerName(),
                        scriptRequest.speakerType(),
                        scriptRequest.text()
                ))
                .toList();
    }

    private List<OptionRequest> toOptionRequests(List<AdminScenarioOptionRequest> optionRequests) {
        if (optionRequests == null) {
            return null;
        }
        return optionRequests.stream()
                .map(optionRequest -> new OptionRequest(
                        optionRequest.label(),
                        optionRequest.nextNodeName()
                ))
                .toList();
    }

    private AdminScenarioDetailResponse toAdminScenarioDetailResponse(
            com.swyp.picke.domain.scenario.dto.response.AdminScenarioDetailResponse legacy
    ) {
        if (legacy == null) {
            return null;
        }

        return AdminScenarioDetailResponse.builder()
                .scenarioId(legacy.scenarioId())
                .battleId(legacy.battleId())
                .title(legacy.title())
                .isInteractive(legacy.isInteractive())
                .nodes(toAdminNodeResponses(legacy.nodes()))
                .voiceSettings(Map.of())
                .build();
    }

    private List<com.swyp.picke.domain.admin.dto.scenario.response.AdminScenarioNodeResponse> toAdminNodeResponses(
            List<com.swyp.picke.domain.scenario.dto.response.NodeResponse> nodes
    ) {
        if (nodes == null) {
            return List.of();
        }
        return nodes.stream()
                .map(node -> com.swyp.picke.domain.admin.dto.scenario.response.AdminScenarioNodeResponse.builder()
                        .nodeId(node.nodeId())
                        .nodeName(node.nodeName())
                        .audioDuration(node.audioDuration())
                        .autoNextNodeId(node.autoNextNodeId())
                        .scripts(toAdminScriptResponses(node.scripts()))
                        .interactiveOptions(toAdminOptionResponses(node.interactiveOptions()))
                        .build())
                .toList();
    }

    private List<com.swyp.picke.domain.admin.dto.scenario.response.AdminScenarioScriptResponse> toAdminScriptResponses(
            List<com.swyp.picke.domain.scenario.dto.response.ScriptResponse> scripts
    ) {
        if (scripts == null) {
            return List.of();
        }
        return scripts.stream()
                .map(script -> com.swyp.picke.domain.admin.dto.scenario.response.AdminScenarioScriptResponse.builder()
                        .scriptId(script.scriptId())
                        .startTimeMs(script.startTimeMs())
                        .speakerType(script.speakerType())
                        .speakerName(script.speakerName())
                        .text(script.text())
                        .build())
                .toList();
    }

    private List<com.swyp.picke.domain.admin.dto.scenario.response.AdminScenarioOptionResponse> toAdminOptionResponses(
            List<com.swyp.picke.domain.scenario.dto.response.OptionResponse> options
    ) {
        if (options == null) {
            return List.of();
        }
        return options.stream()
                .map(option -> com.swyp.picke.domain.admin.dto.scenario.response.AdminScenarioOptionResponse.builder()
                        .label(option.label())
                        .nextNodeId(option.nextNodeId())
                        .build())
                .toList();
    }

    private AdminScenarioResponse toAdminScenarioResponse(
            com.swyp.picke.domain.scenario.dto.response.AdminScenarioResponse legacy
    ) {
        if (legacy == null) {
            return null;
        }
        return new AdminScenarioResponse(
                legacy.scenarioId(),
                legacy.status(),
                legacy.message()
        );
    }

    private AdminDeleteResponse toAdminDeleteResponse(
            com.swyp.picke.domain.scenario.dto.response.AdminDeleteResponse legacy
    ) {
        if (legacy == null) {
            return null;
        }
        return new AdminDeleteResponse(
                legacy.success(),
                legacy.deletedAt()
        );
    }
}

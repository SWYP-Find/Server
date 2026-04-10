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

@Service
@RequiredArgsConstructor
public class AdminScenarioService {

    private final ScenarioService scenarioService;

    public AdminScenarioDetailResponse getScenarioForAdmin(Long battleId) {
        return scenarioService.getScenarioForAdmin(battleId);
    }

    public AdminScenarioCreateResponse createScenario(AdminScenarioCreateRequest request) {
        Long scenarioId = scenarioService.createScenario(toScenarioCreateRequest(request));
        return new AdminScenarioCreateResponse(scenarioId, request.status());
    }

    public void updateScenarioContent(Long scenarioId, AdminScenarioCreateRequest request) {
        scenarioService.updateScenarioContent(scenarioId, toScenarioCreateRequest(request));
    }

    public AdminScenarioResponse updateScenarioStatus(Long scenarioId, ScenarioStatus status) {
        return scenarioService.updateScenarioStatus(scenarioId, status);
    }

    public AdminDeleteResponse deleteScenario(Long scenarioId) {
        return scenarioService.deleteScenario(scenarioId);
    }

    private ScenarioCreateRequest toScenarioCreateRequest(AdminScenarioCreateRequest request) {
        return new ScenarioCreateRequest(
                request.battleId(),
                request.isInteractive(),
                request.status(),
                toNodeRequests(request.nodes()),
                request.voiceSettings()
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
}
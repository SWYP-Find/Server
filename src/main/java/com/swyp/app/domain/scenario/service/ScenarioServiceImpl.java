package com.swyp.app.domain.scenario.service;

import com.swyp.app.domain.battle.entity.Battle;
import com.swyp.app.domain.battle.repository.BattleRepository;
import com.swyp.app.domain.scenario.converter.ScenarioConverter;
import com.swyp.app.domain.scenario.dto.request.NodeRequest;
import com.swyp.app.domain.scenario.dto.request.OptionRequest;
import com.swyp.app.domain.scenario.dto.request.ScenarioCreateRequest;
import com.swyp.app.domain.scenario.dto.request.ScriptRequest;
import com.swyp.app.domain.scenario.dto.response.AdminDeleteResponse;
import com.swyp.app.domain.scenario.dto.response.AdminScenarioDetailResponse;
import com.swyp.app.domain.scenario.dto.response.AdminScenarioResponse;
import com.swyp.app.domain.scenario.dto.response.UserScenarioResponse;
import com.swyp.app.domain.scenario.entity.InteractiveOption;
import com.swyp.app.domain.scenario.entity.Scenario;
import com.swyp.app.domain.scenario.entity.ScenarioNode;
import com.swyp.app.domain.scenario.entity.Script;
import com.swyp.app.domain.scenario.enums.AudioPathType;
import com.swyp.app.domain.scenario.enums.CreatorType;
import com.swyp.app.domain.scenario.enums.ScenarioStatus;
import com.swyp.app.domain.scenario.repository.ScenarioRepository;
import com.swyp.app.domain.vote.entity.Vote;
import com.swyp.app.domain.vote.repository.VoteRepository;
import com.swyp.app.global.common.exception.CustomException;
import com.swyp.app.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScenarioServiceImpl implements ScenarioService {

    private final ScenarioRepository scenarioRepository;
    private final BattleRepository battleRepository;
    private final VoteRepository voteRepository;
    private final ScenarioConverter scenarioConverter;
    private final ScenarioAudioPipelineService audioPipelineService;

    // [유저용] 시나리오 조회 (투표 기반 맞춤 오디오 제공)
    @Override
    public UserScenarioResponse getScenarioForUser(Long battleId, Long userId) {
        Scenario scenario = scenarioRepository.findByBattleIdAndStatus(battleId, ScenarioStatus.PUBLISHED)
                .orElseThrow(() -> new CustomException(ErrorCode.SCENARIO_NOT_FOUND));

        Optional<Vote> optionalVote = voteRepository.findByBattleIdAndUserId(battleId, userId);
        AudioPathType recommendedKey = AudioPathType.COMMON;

        if (optionalVote.isPresent()) {
            Vote vote = optionalVote.get();
            if (scenario.getIsInteractive()) {
                if (vote.getPreVoteOption().getLabel().name().equalsIgnoreCase("A")) {
                    recommendedKey = AudioPathType.PATH_A;
                } else if (vote.getPreVoteOption().getLabel().name().equalsIgnoreCase("B")) {
                    recommendedKey = AudioPathType.PATH_B;
                }
            }
        }

        return scenarioConverter.toUserResponse(scenario, recommendedKey);
    }

    // [관리자용] 배틀 시나리오 전체 조회
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public AdminScenarioDetailResponse getScenarioForAdmin(Long battleId) {
        return scenarioRepository.findByBattleId(battleId)
                .filter(s -> s.getStatus() != ScenarioStatus.ARCHIVED)
                .map(scenarioConverter::toAdminDetailResponse)
                .orElse(null);
    }

    // [관리자] 시나리오 CRUD 로직
    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Long createScenario(ScenarioCreateRequest request) {
        if (scenarioRepository.existsByBattleId(request.battleId())) {
            throw new CustomException(ErrorCode.SCENARIO_ALREADY_EXISTS);
        }

        Battle battle = battleRepository.findById(request.battleId())
                .orElseThrow(() -> new CustomException(ErrorCode.BATTLE_NOT_FOUND));

        Scenario scenario = Scenario.builder()
                .battle(battle)
                .isInteractive(request.isInteractive())
                .status(ScenarioStatus.PENDING)
                .creatorType(CreatorType.ADMIN)
                .build();

        mapAndAddNodesToScenario(scenario, request);
        return scenario.getId();
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void updateScenarioContent(Long scenarioId, ScenarioCreateRequest request) {
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new CustomException(ErrorCode.SCENARIO_NOT_FOUND));

        if (scenario.getStatus() == ScenarioStatus.PUBLISHED) {
            throw new CustomException(ErrorCode.SCENARIO_ALREADY_PUBLISHED);
        }

        scenario.clearNodes();
        scenarioRepository.flush();

        mapAndAddNodesToScenario(scenario, request);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public AdminScenarioResponse updateScenarioStatus(Long scenarioId, ScenarioStatus status) {
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new CustomException(ErrorCode.SCENARIO_NOT_FOUND));

        if (scenario.getStatus() == status) {
            return new AdminScenarioResponse(scenario.getId(), scenario.getStatus(), "이미 처리된 요청입니다.");
        }

        scenario.updateStatus(status);
        scenarioRepository.saveAndFlush(scenario);

        String message = "상태가 " + status + "로 변경되었습니다.";
        if (status == ScenarioStatus.PUBLISHED) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    audioPipelineService.generateAndMergeAudioAsync(scenarioId);
                }
            });
            message = "발행 성공! 오디오 생성이 백그라운드에서 시작되었습니다.";
        }

        return new AdminScenarioResponse(scenario.getId(), scenario.getStatus(), message);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public AdminDeleteResponse deleteScenario(Long scenarioId) {
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new CustomException(ErrorCode.SCENARIO_NOT_FOUND));

        scenario.updateStatus(ScenarioStatus.ARCHIVED);
        return new AdminDeleteResponse(true, LocalDateTime.now());
    }

    private void mapAndAddNodesToScenario(Scenario scenario, ScenarioCreateRequest request) {
        Map<String, ScenarioNode> nodeMap = new HashMap<>();

        for (NodeRequest nodeReq : request.nodes()) {
            ScenarioNode node = ScenarioNode.builder()
                    .nodeName(nodeReq.nodeName())
                    .isStartNode(nodeReq.isStartNode())
                    .audioDuration(0)
                    .build();

            if (nodeReq.scripts() != null) {
                for (ScriptRequest scriptReq : nodeReq.scripts()) {
                    node.addScript(Script.builder()
                            .startTimeMs(0)
                            .speakerType(scriptReq.speakerType())
                            .speakerName(scriptReq.speakerName())
                            .text(scriptReq.text())
                            .build());
                }
            }
            scenario.addNode(node);
        }

        scenarioRepository.saveAndFlush(scenario);

        for (ScenarioNode savedNode : scenario.getNodes()) {
            nodeMap.put(savedNode.getNodeName(), savedNode);
        }

        for (NodeRequest nodeReq : request.nodes()) {
            ScenarioNode parentNode = nodeMap.get(nodeReq.nodeName());

            if (nodeReq.autoNextNode() != null && !nodeReq.autoNextNode().isBlank()) {
                ScenarioNode targetAutoNode = nodeMap.get(nodeReq.autoNextNode());
                if (targetAutoNode != null) {
                    parentNode.updateAutoNextNodeId(targetAutoNode.getId());
                }
            }

            if (nodeReq.interactiveOptions() != null) {
                for (OptionRequest optReq : nodeReq.interactiveOptions()) {
                    ScenarioNode targetNode = nodeMap.get(optReq.nextNodeName());
                    if (targetNode != null) {
                        parentNode.addOption(InteractiveOption.builder()
                                .label(optReq.label())
                                .nextNodeId(targetNode.getId())
                                .build());
                    }
                }
            }
        }

        scenarioRepository.flush();
    }
}
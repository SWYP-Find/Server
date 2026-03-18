package com.swyp.app.domain.scenario.service;

import com.swyp.app.domain.battle.entity.Battle;
import com.swyp.app.domain.battle.repository.BattleRepository;
import com.swyp.app.domain.scenario.converter.ScenarioConverter;
import com.swyp.app.domain.scenario.dto.request.NodeRequest;
import com.swyp.app.domain.scenario.dto.request.OptionRequest;
import com.swyp.app.domain.scenario.dto.request.ScenarioCreateRequest;
import com.swyp.app.domain.scenario.dto.request.ScriptRequest;
import com.swyp.app.domain.scenario.dto.response.AdminDeleteResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScenarioServiceImpl implements ScenarioService {

    private final ScenarioRepository scenarioRepository;
    private final BattleRepository battleRepository;
    private final VoteRepository voteRepository;
    private final ScenarioConverter scenarioConverter;
    private final ScenarioAudioPipelineService audioPipelineService;

    @Override
    public UserScenarioResponse getScenarioForUser(UUID battleId, Long userId) {
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

    @Override
    @Transactional
    public UUID createScenario(ScenarioCreateRequest request) {
        if (scenarioRepository.existsByBattleId(request.battleId())) {
            throw new CustomException(ErrorCode.SCENARIO_ALREADY_EXISTS);
        }

        Battle battle = battleRepository.findById(request.battleId())
                .orElseThrow(() -> new CustomException(ErrorCode.BATTLE_NOT_FOUND));

        Scenario scenario = Scenario.builder()
                .battle(battle)
                .isInteractive(request.isInteractive())
                .status(ScenarioStatus.DRAFT)
                .creatorType(CreatorType.ADMIN)
                .build();

        // 중복 방지를 위해 공통 매핑 메서드 사용
        mapAndAddNodesToScenario(scenario, request);
        return scenario.getId();
    }

    @Override
    @Transactional
    public void updateScenarioContent(UUID scenarioId, ScenarioCreateRequest request) {
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new CustomException(ErrorCode.SCENARIO_NOT_FOUND));

        if (scenario.getStatus() == ScenarioStatus.PUBLISHED) {
            throw new CustomException(ErrorCode.SCENARIO_ALREADY_PUBLISHED);
        }

        scenario.clearNodes();
        scenarioRepository.flush(); // 기존 노드 삭제를 먼저 DB에 반영

        mapAndAddNodesToScenario(scenario, request);
    }

    @Override
    @Transactional
    public AdminScenarioResponse updateScenarioStatus(UUID scenarioId, ScenarioStatus status) {
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new CustomException(ErrorCode.SCENARIO_NOT_FOUND));

        // 이미 바꾸려는 상태(PUBLISHED)와 같다면, 이미 처리되었다고 응답 (API 중복 호출 차단)
        if (scenario.getStatus() == status) {
            return new AdminScenarioResponse(scenario.getId(), scenario.getStatus(), "이미 처리된 요청입니다.");
        }

        scenario.updateStatus(status);
        scenarioRepository.saveAndFlush(scenario); // DB 상태 변경 확정

        String message = "상태가 " + status + "로 변경되었습니다.";
        if (status == ScenarioStatus.PUBLISHED) {
            // 트랜잭션이 완전히 '커밋'된 후에 비동기 쓰레드가 돌도록 예약!
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
    public AdminDeleteResponse deleteScenario(UUID scenarioId) {
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new CustomException(ErrorCode.SCENARIO_NOT_FOUND));

        scenario.updateStatus(ScenarioStatus.ARCHIVED);
        return new AdminDeleteResponse(true, LocalDateTime.now());
    }

    /**
     * 노드 생성 및 연결 로직 (create와 update에서 공통 사용)
     */
    private void mapAndAddNodesToScenario(Scenario scenario, ScenarioCreateRequest request) {
        Map<String, ScenarioNode> nodeMap = new HashMap<>();

        // 1. 노드와 스크립트를 먼저 생성
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

        // DB에 저장해서 모든 노드의 UUID를 발급받습니다.
        scenarioRepository.saveAndFlush(scenario);

        // 발급받은 ID를 가진 노드들로 nodeMap을 다시 만듭니다.
        for (ScenarioNode savedNode : scenario.getNodes()) {
            nodeMap.put(savedNode.getNodeName(), savedNode);
        }

        // 이제 ID를 바탕으로 경로(NextNodeId)를 연결합니다.
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

        // 마지막으로 연결 정보를 DB에 확실히 반영
        scenarioRepository.flush();
    }
}
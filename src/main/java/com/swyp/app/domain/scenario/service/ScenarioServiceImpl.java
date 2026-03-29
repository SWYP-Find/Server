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
import com.swyp.app.global.infra.s3.service.S3UploadService;
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
    private final S3UploadService s3Service;

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
                .status(request.status())
                .creatorType(CreatorType.ADMIN)
                .build();

        // 1. 부모 시나리오 먼저 저장 (ID 발급)
        scenarioRepository.save(scenario);

        // 2. 노드 및 스크립트 저장 (이때 모든 Script의 audioUrl은 null 상태)
        smartUpdateNodesToScenario(scenario, request);

        // 3. 오디오 파이프라인 호출 (트랜잭션 커밋 후 비동기 실행)
        triggerAudioPipeline(scenario.getId());

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

        // 스마트 업데이트 로직 호출 (수정 사항이 있었는지 boolean으로 반환받음)
        boolean isModified = smartUpdateNodesToScenario(scenario, request);

        // 대본 내용(노드, 대사 등)이 하나라도 바뀌었다면?
        if (isModified) {
            // 1. 기존에 만들어둔 '최종 병합 오디오(A루트, B루트 등)'를 S3에서 전부 삭제!
            for (String mergedAudioUrl : scenario.getAudios().values()) {
                if (mergedAudioUrl != null) s3Service.deleteFile(mergedAudioUrl);
            }
            // 2. DB에서 최종 오디오 URL 초기화
            scenario.clearAudios();
        }
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public AdminScenarioResponse updateScenarioStatus(Long scenarioId, ScenarioStatus status) {
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new CustomException(ErrorCode.SCENARIO_NOT_FOUND));

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

    // 부분 업데이트 및 노드 삭제 시 S3 정리 로직
    private boolean smartUpdateNodesToScenario(Scenario scenario, ScenarioCreateRequest request) {
        boolean isModified = false;
        Map<String, ScenarioNode> existingNodeMap = new HashMap<>();
        for (ScenarioNode node : scenario.getNodes()) {
            existingNodeMap.put(node.getNodeName(), node);
        }

        Map<String, ScenarioNode> updatedNodeMap = new HashMap<>();
        for (NodeRequest nodeReq : request.nodes()) {
            ScenarioNode existingNode = existingNodeMap.get(nodeReq.nodeName());

            if (existingNode != null) {
                existingNode.updateBasicInfo(nodeReq.isStartNode());

                // 대사 변경 여부 체크
                boolean scriptChanged = updateScriptsSmartly(existingNode, nodeReq.scripts());
                if (scriptChanged) isModified = true;

                updatedNodeMap.put(existingNode.getNodeName(), existingNode);
                existingNode.clearOptionsAndLinks();
            } else {
                isModified = true; // 새 노드 생성됨
                ScenarioNode newNode = ScenarioNode.builder()
                        .nodeName(nodeReq.nodeName())
                        .isStartNode(nodeReq.isStartNode())
                        .audioDuration(0)
                        .build();

                if (nodeReq.scripts() != null) {
                    for (ScriptRequest scriptReq : nodeReq.scripts()) {
                        newNode.addScript(Script.builder()
                                .startTimeMs(0)
                                .speakerType(scriptReq.speakerType())
                                .speakerName(scriptReq.speakerName())
                                .text(scriptReq.text())
                                .build());
                    }
                }
                scenario.addNode(newNode);
                updatedNodeMap.put(newNode.getNodeName(), newNode);
            }
        }

        // 노드가 삭제될 때, 그 안에 있던 개별 대사의 오디오 파일도 S3에서 삭제
        boolean nodesRemoved = scenario.getNodes().removeIf(node -> {
            boolean shouldRemove = !updatedNodeMap.containsKey(node.getNodeName());
            if (shouldRemove) {
                for (Script script : node.getScripts()) {
                    if (script.getAudioUrl() != null) {
                        s3Service.deleteFile(script.getAudioUrl()); // S3에서 삭제
                    }
                }
            }
            return shouldRemove;
        });

        if (nodesRemoved) isModified = true;
        scenarioRepository.flush();

        // 링크 재구축 로직
        for (NodeRequest nodeReq : request.nodes()) {
            ScenarioNode parentNode = updatedNodeMap.get(nodeReq.nodeName());
            if (nodeReq.autoNextNode() != null && !nodeReq.autoNextNode().isBlank()) {
                Optional.ofNullable(updatedNodeMap.get(nodeReq.autoNextNode()))
                        .ifPresent(target -> parentNode.updateAutoNextNodeId(target.getId()));
            }
            if (nodeReq.interactiveOptions() != null) {
                for (OptionRequest optReq : nodeReq.interactiveOptions()) {
                    Optional.ofNullable(updatedNodeMap.get(optReq.nextNodeName()))
                            .ifPresent(target -> parentNode.addOption(InteractiveOption.builder()
                                    .label(optReq.label())
                                    .nextNodeId(target.getId())
                                    .build()));
                }
            }
        }
        scenarioRepository.flush();
        return isModified;
    }

    /**
     * 공통 로직: 트랜잭션이 성공적으로 DB에 반영(Commit)된 후 비동기 오디오 작업 시작
     */
    private void triggerAudioPipeline(Long scenarioId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                audioPipelineService.generateAndMergeAudioAsync(scenarioId);
            }
        });
    }

    // 텍스트 변경 및 대사 삭제 시 S3 정리 로직
    private boolean updateScriptsSmartly(ScenarioNode existingNode, java.util.List<ScriptRequest> requestedScripts) {
        boolean isModified = false;
        if (requestedScripts == null) return false;
        java.util.List<Script> existingScripts = existingNode.getScripts();

        for (int i = 0; i < requestedScripts.size(); i++) {
            ScriptRequest reqScript = requestedScripts.get(i);

            if (i < existingScripts.size()) {
                Script existingScript = existingScripts.get(i);

                // 텍스트가 달라졌다면?
                if (!existingScript.getText().equals(reqScript.text())) {
                    isModified = true;
                    // 1. S3에서 기존 오디오 조각 파일 완전히 삭제
                    if (existingScript.getAudioUrl() != null) {
                        s3Service.deleteFile(existingScript.getAudioUrl());
                    }
                    // 2. DB 업데이트 및 audioUrl null 처리 (엔티티의 updateContent 호출)
                    existingScript.updateContent(
                            reqScript.speakerType(),
                            reqScript.speakerName(),
                            reqScript.text()
                    );
                }
            } else {
                isModified = true; // 새 대사 추가됨
                existingNode.addScript(Script.builder()
                        .startTimeMs(0)
                        .speakerType(reqScript.speakerType())
                        .speakerName(reqScript.speakerName())
                        .text(reqScript.text())
                        .build());
            }
        }

        // 기존 대사가 삭제된 경우 (요청보다 기존 스크립트가 많을 때)
        while (existingScripts.size() > requestedScripts.size()) {
            isModified = true;
            Script removedScript = existingScripts.remove(existingScripts.size() - 1);
            // S3에서 불필요해진 오디오 파일 삭제
            if (removedScript.getAudioUrl() != null) {
                s3Service.deleteFile(removedScript.getAudioUrl());
            }
        }

        return isModified;
    }
}
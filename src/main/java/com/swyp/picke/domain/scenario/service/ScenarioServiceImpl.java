package com.swyp.picke.domain.scenario.service;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.battle.repository.BattleOptionRepository;
import com.swyp.picke.domain.battle.repository.BattleRepository;
import com.swyp.picke.domain.scenario.converter.ScenarioConverter;
import com.swyp.picke.domain.scenario.dto.request.NodeRequest;
import com.swyp.picke.domain.scenario.dto.request.OptionRequest;
import com.swyp.picke.domain.scenario.dto.request.ScenarioCreateRequest;
import com.swyp.picke.domain.scenario.dto.request.ScriptRequest;
import com.swyp.picke.domain.admin.dto.scenario.response.AdminDeleteResponse;
import com.swyp.picke.domain.admin.dto.scenario.response.AdminScenarioDetailResponse;
import com.swyp.picke.domain.admin.dto.scenario.response.AdminScenarioResponse;
import com.swyp.picke.domain.scenario.dto.response.UserScenarioResponse;
import com.swyp.picke.domain.scenario.entity.InteractiveOption;
import com.swyp.picke.domain.scenario.entity.Scenario;
import com.swyp.picke.domain.scenario.entity.ScenarioNode;
import com.swyp.picke.domain.scenario.entity.Script;
import com.swyp.picke.domain.scenario.enums.AudioPathType;
import com.swyp.picke.domain.scenario.enums.CreatorType;
import com.swyp.picke.domain.scenario.enums.ScenarioStatus;
import com.swyp.picke.domain.scenario.enums.SpeakerType;
import com.swyp.picke.domain.scenario.repository.ScenarioRepository;
import com.swyp.picke.domain.vote.entity.BattleVote;
import com.swyp.picke.domain.vote.repository.BattleVoteRepository;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import com.swyp.picke.global.infra.s3.service.S3UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScenarioServiceImpl implements ScenarioService {

    private final ScenarioRepository scenarioRepository;
    private final BattleRepository battleRepository;
    private final BattleVoteRepository BattleVoteRepository;
    private final ScenarioConverter scenarioConverter;
    private final ScenarioAudioPipelineService audioPipelineService;
    private final S3UploadService s3Service;
    private final BattleOptionRepository battleOptionRepository;

    @Override
    public UserScenarioResponse getScenarioForUser(Long battleId, Long userId) {
        Scenario scenario = scenarioRepository.findByBattleIdAndStatus(battleId, ScenarioStatus.PUBLISHED)
                .orElseThrow(() -> new CustomException(ErrorCode.SCENARIO_NOT_FOUND));

        Optional<BattleVote> optionalVote = BattleVoteRepository.findByBattleIdAndUserId(battleId, userId);
        AudioPathType recommendedKey = AudioPathType.COMMON;

        if (optionalVote.isPresent()) {
            BattleVote BattleVote = optionalVote.get();
            if (scenario.getIsInteractive()) {
                if (BattleVote.getPreVoteOption().getLabel().name().equalsIgnoreCase("A")) {
                    recommendedKey = AudioPathType.PATH_A;
                } else if (BattleVote.getPreVoteOption().getLabel().name().equalsIgnoreCase("B")) {
                    recommendedKey = AudioPathType.PATH_B;
                }
            }
        }

        return scenarioConverter.toUserResponse(scenario, recommendedKey);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public AdminScenarioDetailResponse getScenarioForAdmin(Long battleId) {
        return scenarioRepository.findByBattleId(battleId)
                .filter(s -> s.getStatus() != ScenarioStatus.ARCHIVED)
                .map(scenarioConverter::toAdminDetailResponse)
                .orElse(null);
    }

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
        scenario.replaceVoiceSettings(normalizeVoiceSettings(request.voiceSettings()));

        scenarioRepository.save(scenario);

        Map<String, String> speakerMap = createSpeakerMap(battle);
        smartUpdateNodesToScenario(scenario, request, speakerMap);

        if (request.status() == ScenarioStatus.PUBLISHED) {
            triggerAudioPipeline(scenario.getId());
        }

        return scenario.getId();
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void updateScenarioContent(Long scenarioId, ScenarioCreateRequest request) {
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new CustomException(ErrorCode.SCENARIO_NOT_FOUND));

        Map<String, String> speakerMap = createSpeakerMap(scenario.getBattle());
        boolean scriptOrNodeModified = smartUpdateNodesToScenario(scenario, request, speakerMap);
        Set<SpeakerType> changedSpeakers = updateVoiceSettings(scenario, request.voiceSettings());
        if (!changedSpeakers.isEmpty()) {
            invalidateScriptAudiosBySpeaker(scenario, changedSpeakers);
        }
        boolean isModified = scriptOrNodeModified || !changedSpeakers.isEmpty();

        if (isModified) {
            for (String mergedAudioUrl : scenario.getAudios().values()) {
                if (mergedAudioUrl != null) s3Service.deleteFile(mergedAudioUrl);
            }
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
    private boolean smartUpdateNodesToScenario(Scenario scenario, ScenarioCreateRequest request, Map<String, String> speakerMap) {
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
                boolean scriptChanged = updateScriptsSmartly(existingNode, nodeReq.scripts(), speakerMap);
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
                        String speakerKey = scriptReq.speakerType() == null ? null : scriptReq.speakerType().name();
                        String autoSpeakerName = speakerMap.getOrDefault(speakerKey, scriptReq.speakerName());
                        newNode.addScript(Script.builder()
                                .startTimeMs(0)
                                .speakerType(scriptReq.speakerType())
                                .speakerName(autoSpeakerName)
                                .text(scriptReq.text())
                                .build());
                    }
                }
                scenario.addNode(newNode);
                updatedNodeMap.put(newNode.getNodeName(), newNode);
            }
        }

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

    private void triggerAudioPipeline(Long scenarioId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                audioPipelineService.generateAndMergeAudioAsync(scenarioId);
            }
        });
    }

    private boolean updateScriptsSmartly(ScenarioNode existingNode, java.util.List<ScriptRequest> requestedScripts, Map<String, String> speakerMap) {
        boolean isModified = false;
        if (requestedScripts == null) return false;
        java.util.List<Script> existingScripts = existingNode.getScripts();

        for (int i = 0; i < requestedScripts.size(); i++) {
            ScriptRequest reqScript = requestedScripts.get(i);
            String speakerKey = reqScript.speakerType() == null ? null : reqScript.speakerType().name();
            String autoSpeakerName = speakerMap.getOrDefault(speakerKey, reqScript.speakerName());

            if (i < existingScripts.size()) {
                Script existingScript = existingScripts.get(i);

                if (!Objects.equals(existingScript.getText(), reqScript.text())
                        || !Objects.equals(existingScript.getSpeakerName(), autoSpeakerName)
                        || existingScript.getSpeakerType() != reqScript.speakerType()) {
                    isModified = true;
                    if (existingScript.getAudioUrl() != null) {
                        s3Service.deleteFile(existingScript.getAudioUrl());
                    }
                    existingScript.updateContent(
                            reqScript.speakerType(),
                            autoSpeakerName,
                            reqScript.text()
                    );
                }
            } else {
                isModified = true; // 새 대사 추가됨
                existingNode.addScript(Script.builder()
                        .startTimeMs(0)
                        .speakerType(reqScript.speakerType())
                        .speakerName(autoSpeakerName)
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

    private Map<String, String> createSpeakerMap(Battle battle) {
        Map<String, String> map = new HashMap<>();
        map.put("NARRATOR", "나레이터");
        List<BattleOption> options = battleOptionRepository.findByBattle(battle);

        if (options != null) {
            for (BattleOption option : options) {
                String key = String.valueOf(option.getLabel());
                map.put(key, option.getRepresentative());
            }
        }

        return map;
    }

    private Set<SpeakerType> updateVoiceSettings(Scenario scenario, Map<SpeakerType, String> requestedVoiceSettings) {
        Map<SpeakerType, String> normalizedRequested = normalizeVoiceSettings(requestedVoiceSettings);
        Map<SpeakerType, String> current = scenario.getVoiceSettings();
        Set<SpeakerType> changedSpeakers = new HashSet<>();

        for (SpeakerType speakerType : SpeakerType.values()) {
            if (!Objects.equals(current.get(speakerType), normalizedRequested.get(speakerType))) {
                changedSpeakers.add(speakerType);
            }
        }

        if (!changedSpeakers.isEmpty()) {
            scenario.replaceVoiceSettings(normalizedRequested);
        }
        return changedSpeakers;
    }

    private void invalidateScriptAudiosBySpeaker(Scenario scenario, Set<SpeakerType> changedSpeakers) {
        for (ScenarioNode node : scenario.getNodes()) {
            for (Script script : node.getScripts()) {
                if (!changedSpeakers.contains(script.getSpeakerType())) continue;
                if (script.getAudioUrl() != null) {
                    s3Service.deleteFile(script.getAudioUrl());
                    script.updateAudioUrl(null);
                }
            }
        }
    }

    private Map<SpeakerType, String> normalizeVoiceSettings(Map<SpeakerType, String> requestedVoiceSettings) {
        Map<SpeakerType, String> normalized = new EnumMap<>(SpeakerType.class);
        if (requestedVoiceSettings == null || requestedVoiceSettings.isEmpty()) {
            return normalized;
        }

        for (Map.Entry<SpeakerType, String> entry : requestedVoiceSettings.entrySet()) {
            SpeakerType speakerType = entry.getKey();
            if (speakerType == null) continue;

            String voiceCode = entry.getValue();
            if (voiceCode == null) continue;

            String trimmed = voiceCode.trim();
            if (!trimmed.isEmpty()) {
                normalized.put(speakerType, trimmed);
            }
        }
        return normalized;
    }
}

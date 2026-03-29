package com.swyp.app.domain.scenario.service;

import com.swyp.app.domain.scenario.entity.*;
import com.swyp.app.domain.scenario.enums.*;
import com.swyp.app.domain.scenario.repository.ScenarioRepository;
import com.swyp.app.global.util.PathFinder;
import com.swyp.app.global.infra.media.service.FFmpegService;
import com.swyp.app.global.infra.s3.enums.FileCategory;
import com.swyp.app.global.infra.s3.service.S3UploadService;
import com.swyp.app.global.infra.tts.service.TtsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScenarioAudioPipelineService {

    private final ScenarioRepository scenarioRepository;
    private final TtsService ttsService;
    private final FFmpegService ffmpegService;
    private final S3UploadService s3UploadService;

    private static final int SILENCE_MS = 600;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void generateAndMergeAudioAsync(Long scenarioId) {

        Scenario scenario = scenarioRepository.findById(scenarioId).orElseThrow();

        log.info("\n==================================================");
        log.info("[오디오 파이프라인 시작] 시나리오 ID: {}", scenarioId);
        log.info("[시나리오 타입] 인터랙티브(분기) 여부: {}", scenario.getIsInteractive());

        try {
            log.info("--- [1단계] TTS API 호출 및 캐싱 (S3 조각 활용) ---");
            Map<Long, File> ttsCache = new HashMap<>();
            int apiCallCount = 0;

            for (ScenarioNode node : scenario.getNodes()) {
                for (Script script : node.getScripts()) {
                    File audioFile;

                    // 1. 텍스트가 안 바뀌어서 DB에 S3 URL이 살아있다면? (재사용)
                    if (script.getAudioUrl() != null) {
                        log.info(">> 기존 오디오 재사용 (S3 다운로드): 스크립트 ID {}", script.getId());
                        audioFile = s3UploadService.downloadFile(script.getAudioUrl());
                    }
                    // 2. 텍스트가 바뀌었거나 새로 추가되었다면? (새로 생성 후 S3에 저장)
                    else {
                        log.info(">> 새 오디오 생성 (TTS API 호출): 스크립트 ID {}", script.getId());
                        audioFile = ttsService.generateTtsWithSsml(script.getText(), script.getSpeakerType());

                        // 새로 만든 조각 파일을 다음 수정을 위해 S3에 업로드 (chunks 폴더)
                        String chunkKey = FileCategory.SCENARIO.getPath() + "/chunks/" + UUID.randomUUID() + ".mp3";
                        String chunkUrl = s3UploadService.uploadFile(chunkKey, audioFile);

                        // DB 엔티티에 새로 만든 S3 주소 기록 (dirty checking으로 자동 저장됨)
                        script.updateAudioUrl(chunkUrl);

                        apiCallCount++;
                    }

                    ttsCache.put(script.getId(), audioFile);
                }
            }
            log.info("[API 호출 통계] 💳 TTS API가 총 {}회 호출되어 캐시에 저장되었습니다.", apiCallCount);
            File silence = ffmpegService.createSilenceFile(SILENCE_MS);

            // 경로 탐색
            List<List<ScenarioNode>> paths = PathFinder.findAllPaths(scenario.getNodes());
            log.info("--- [2단계] 경로 탐색 완료. 총 {}개의 통합 경로 발견 ---", paths.size());

            for (int i = 0; i < paths.size(); i++) {
                log.info(">> 경로 {}번 처리 시작...", (i + 1));
                processPathAndMerge(scenario, paths.get(i), ttsCache, silence);
            }

            // 최종 상태 및 시간 계산 결과 저장
            scenario.updateStatus(ScenarioStatus.PUBLISHED);
            scenarioRepository.saveAndFlush(scenario);

            cleanUpFiles(ttsCache, silence);
            log.info("[오디오 파이프라인 종료] 시나리오 생성 및 S3 업로드 완벽 성공!");
            log.info("==================================================\n");

        } catch (Exception e) {
            log.error("[오디오 파이프라인 치명적 오류]", e);
        }
    }

    private void processPathAndMerge(Scenario scenario, List<ScenarioNode> path, Map<Long, File> cache, File silence) throws Exception {
        List<File> segments = new ArrayList<>();
        int currentTimeMs = 0;

        int commonAudioCount = 0;
        int aAudioCount = 0;
        int bAudioCount = 0;

        AudioPathType type = determineType(path, scenario.getIsInteractive());

        for (ScenarioNode node : path) {
            int nodeDurationMs = 0;
            log.info("   -> 방문 중인 노드: {}", node.getNodeName());

            boolean isNodeA = node.getNodeName().toUpperCase().contains("_A");
            boolean isNodeB = node.getNodeName().toUpperCase().contains("_B");
            boolean isCommonNode = !scenario.getIsInteractive() || (!isNodeA && !isNodeB);

            for (Script script : node.getScripts()) {
                script.updateStartTimeMs(currentTimeMs);
                File audio = cache.get(script.getId());

                segments.add(audio);
                segments.add(silence);

                if (isCommonNode) commonAudioCount++;
                else if (isNodeA) aAudioCount++;
                else if (isNodeB) bAudioCount++;

                int duration = ffmpegService.getAudioDurationMs(audio) + SILENCE_MS;
                currentTimeMs += duration;
                nodeDurationMs += duration;
            }
            node.updateAudioDuration(nodeDurationMs / 1000);
        }

        if (type == AudioPathType.PATH_A) {
            log.info("[FFmpeg 조립] 'PATH_A' 경로 분석: 공통 오디오 {}개 + A 전용 오디오 {}개 병합 완료", commonAudioCount, aAudioCount);
        } else if (type == AudioPathType.PATH_B) {
            log.info("[FFmpeg 조립] 'PATH_B' 경로 분석: 공통 오디오 {}개 + B 전용 오디오 {}개 병합 완료", commonAudioCount, bAudioCount);
        } else {
            log.info("[FFmpeg 조립] 'COMMON' 경로 분석: 공통 오디오 {}개 병합 완료", commonAudioCount);
        }

        File merged = ffmpegService.mergeAudioFiles(segments);
        String s3Key = FileCategory.SCENARIO.getPath() + "/" + scenario.getId() + "/" + type + ".mp3";
        String url = s3UploadService.uploadFile(s3Key, merged);

        log.info("[S3 업로드 완료] {} 오디오 주소: {}", type, url);

        scenario.addAudioUrl(type, url);
    }

    private AudioPathType determineType(List<ScenarioNode> path, boolean isInteractive) {
        if (!isInteractive) return AudioPathType.COMMON;
        for (ScenarioNode n : path) {
            if (n.getNodeName().toUpperCase().contains("_A")) return AudioPathType.PATH_A;
            if (n.getNodeName().toUpperCase().contains("_B")) return AudioPathType.PATH_B;
        }
        return AudioPathType.COMMON;
    }

    private void cleanUpFiles(Map<Long, File> cache, File silence) {
        cache.values().forEach(File::delete);
        if (silence != null) silence.delete();
    }
}
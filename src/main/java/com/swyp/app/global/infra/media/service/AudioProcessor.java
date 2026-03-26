package com.swyp.app.global.infra.media.service;

import java.io.File;
import java.util.List;

/**
 * 오디오 처리 관련 기능을 정의하는 인터페이스
 * - 오디오 파일 길이 측정 및 여러 오디오 파일 병합 기능 제공
 */
public interface AudioProcessor {
    Integer getAudioDurationMs(File file) throws Exception;
    File mergeAudioFiles(List<File> files) throws Exception;
}
package com.swyp.app.domain.scenario.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * S3 업로드 모의(Mock) 서비스
 * - 실제 S3 인프라가 구성되기 전, 로컬 환경에서 업로드 성공으로 간주하고 가상 URL을 반환하기 위해 사용
 */
@Slf4j
@Primary // 인프라 연결 전 테스트용
@Service
public class MockS3UploadServiceImpl implements S3UploadService {
    @Override
    public String uploadFile(String key, File file) {
        log.info("[Mock S3] 업로드 시뮬레이션 완료. 키: {}", key);
        return "https://mock-s3.amazonaws.com/" + key;
    }
}
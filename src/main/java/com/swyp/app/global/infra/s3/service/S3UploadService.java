package com.swyp.app.global.infra.s3.service;

import java.io.File;
import java.time.Duration;

/**
 * S3 파일 업로드 관련 기능을 정의하는 인터페이스
 */
public interface S3UploadService {
    String uploadFile(String key, File file);
    String getPresignedUrl(String key, Duration duration);
    void deleteFile(String fileUrl);
    File downloadFile(String fileUrl);
}
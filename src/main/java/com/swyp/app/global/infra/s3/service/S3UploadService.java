package com.swyp.app.global.infra.s3.service;

import java.io.File;

/**
 * S3 파일 업로드 관련 기능을 정의하는 인터페이스
 */
public interface S3UploadService {
    String uploadFile(String key, File file);
}
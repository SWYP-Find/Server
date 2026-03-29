package com.swyp.app.global.infra.s3.service;

import com.swyp.app.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

@Slf4j
@Primary
@Service
@RequiredArgsConstructor
public class S3UploadServiceImpl implements S3UploadService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner; // 보안 URL 생성을 위한 프레시그너 추가

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;

    /**
     * S3 파일 업로드
     * @return 저장된 파일의 'Key(경로)' 또는 필요 시 전체 URL
     */
    @Override
    public String uploadFile(String key, File file) {
        if (file == null || !file.exists()) {
            throw new RuntimeException(ErrorCode.FILE_NOT_FOUND.getMessage());
        }

        try {
            log.info("[AWS S3] 업로드 시작 - 버킷: {}, 키: {}", bucketName, key);

            // Content-Type 자동 감지 (오디오 등 확장자 대응)
            String contentType = Files.probeContentType(file.toPath());
            if (contentType == null) {
                contentType = determineContentType(key);
            }

            // S3 업로드 요청 생성
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromFile(file));
            log.info("[AWS S3] 업로드 완료! 키: {}, Content-Type: {}", key, contentType);
            return key;

        } catch (Exception e) {
            log.error("[AWS S3] 업로드 실패 - 키: {}", key, e);
            throw new RuntimeException(ErrorCode.FILE_UPLOAD_FAILED.getMessage());
        }
    }

    /**
     * S3에서 파일을 다운로드하여 로컬 임시 파일로 반환
     */
    @Override
    public File downloadFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            throw new RuntimeException("다운로드할 S3 URL이 없습니다.");
        }

        try {
            // URL에서 순수 Key만 추출
            String pureKey = fileUrl.contains(".com/") ? fileUrl.split(".com/")[1] : fileUrl;

            // 다운로드 받을 로컬 임시 파일 생성
            File tempFile = File.createTempFile("s3_download_", ".mp3");
            Path tempFilePath = tempFile.toPath();

            // S3 다운로드 요청
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(pureKey)
                    .build();

            // S3에서 파일을 읽어서 로컬 임시 파일에 쓰기
            s3Client.getObject(getObjectRequest, tempFilePath);

            return tempFile; // FFmpeg 병합 완료 후 cleanUpFiles에서 알아서 지워짐

        } catch (Exception e) {
            log.error("[AWS S3] 파일 다운로드 실패 - URL: {}", fileUrl, e);
            throw new RuntimeException("S3 오디오 조각 다운로드 실패", e);
        }
    }

    /**
     * 관리자 전용: 특정 시점에만 유효한 임시 보안 URL 생성 (Presigned URL)
     * @param key S3에 저장된 파일의 경로 (예: images/battles/uuid.png)
     * @param durationUrl 유효 시간 (예: Duration.ofMinutes(10))
     */
    public String getPresignedUrl(String key, Duration durationUrl) {
        if (key == null || key.isEmpty()) return null;

        // URL에서 도메인을 제외한 순수 'Key'만 추출 (만약 전체 URL이 들어올 경우를 대비)
        String pureKey = key.contains(".com/") ? key.split(".com/")[1] : key;

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(durationUrl)
                .getObjectRequest(builder -> builder.bucket(bucketName).key(pureKey))
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    private String determineContentType(String key) {
        if (key.endsWith(".mp3")) return "audio/mpeg";
        if (key.endsWith(".png")) return "image/png";
        if (key.endsWith(".jpg") || key.endsWith(".jpeg")) return "image/jpeg";
        return "application/octet-stream";
    }

    private void deleteLocalFile(File file) {
        if (file != null && file.exists()) {
            if (!file.delete()) {
                log.warn("[로컬 파일 삭제 실패]: {}", file.getAbsolutePath());
            }
        }
    }

    /**
     * S3 파일 삭제
     * @param fileUrl 삭제할 파일의 전체 URL 또는 Key
     */
    @Override
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }

        try {
            // 1. 전체 URL에서 순수 Key 추출 (기존 getPresignedUrl에 있던 방식과 동일하게 처리)
            String pureKey = fileUrl.contains(".com/") ? fileUrl.split(".com/")[1] : fileUrl;

            // 2. AWS SDK v2 전용 삭제 요청 객체 생성
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(pureKey)
                    .build();

            // 3. S3에서 파일 삭제 실행
            s3Client.deleteObject(deleteObjectRequest);
            log.info("[AWS S3] 파일 삭제 성공: {}", pureKey);

        } catch (Exception e) {
            // 파일 삭제 실패가 전체 서비스(수정 로직 등)의 예외(Rollback)로 번지지 않도록 로그만 남깁니다.
            log.error("[AWS S3] 파일 삭제 실패 - URL: {}", fileUrl, e);
        }
    }
}
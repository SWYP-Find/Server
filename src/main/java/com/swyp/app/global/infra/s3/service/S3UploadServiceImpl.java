package com.swyp.app.global.infra.s3.service; // 패키지 이동됨

import com.swyp.app.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.nio.file.Files;

@Slf4j
@Primary
@Service
@RequiredArgsConstructor
public class S3UploadServiceImpl implements S3UploadService {

    private final S3Client s3Client;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;

    @Override
    public String uploadFile(String key, File file) {
        if (file == null || !file.exists()) {
            throw new RuntimeException(ErrorCode.FILE_NOT_FOUND.getMessage());
        }

        try {
            log.info("[AWS S3] 파일 업로드 시작... 버킷: {}, 키: {}", bucketName, key);

            String contentType = Files.probeContentType(file.toPath());
            if (contentType == null) {
                contentType = key.endsWith(".mp3") ? "audio/mpeg" : "application/octet-stream";
            }

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromFile(file));

            log.info("[AWS S3] 업로드 완료! 키: {}, Content-Type: {}", key, contentType);

            return key;

        } catch (Exception e) {
            log.error("[AWS S3] 파일 업로드 실패 - 키: {}", key, e);
            throw new RuntimeException(ErrorCode.FILE_UPLOAD_FAILED.getMessage());
        } finally {
            if (file.exists()) {
                boolean deleted = file.delete();
                if (!deleted) {
                    log.warn("[로컬 파일 삭제 실패] 용량 확보를 위해 확인 필요: {}", file.getAbsolutePath());
                }
            }
        }
    }
}
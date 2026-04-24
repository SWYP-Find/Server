package com.swyp.picke.global.infra.s3.service;

import com.swyp.picke.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Map;

@Slf4j
@Primary
@Service
@RequiredArgsConstructor
public class S3UploadServiceImpl implements S3UploadService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;

    @Override
    public String uploadFile(String key, File file) {
        if (file == null || !file.exists()) {
            throw new RuntimeException(ErrorCode.FILE_NOT_FOUND.getMessage());
        }

        try {
            String normalizedKey = extractKey(key);
            log.info("[AWS S3] Upload start - bucket: {}, key: {}", bucketName, normalizedKey);

            // Reuse immediately when key already exists (same file name/path).
            if (objectExists(normalizedKey)) {
                log.info("[AWS S3] Reusing existing object by key: {}", normalizedKey);
                return normalizedKey;
            }

            String contentType = Files.probeContentType(file.toPath());
            if (contentType == null) {
                contentType = determineContentType(normalizedKey);
            }

            // Reuse existing key when same content hash already exists in this prefix.
            String sha256 = calculateSha256(file.toPath());
            String md5 = calculateMd5(file.toPath());
            String prefix = extractPrefix(normalizedKey);
            String existingSameContentKey = findExistingKeyByContentDigest(prefix, sha256, md5);
            if (existingSameContentKey != null) {
                log.info("[AWS S3] Reusing existing object by content hash: {}", existingSameContentKey);
                return existingSameContentKey;
            }

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(normalizedKey)
                    .contentType(contentType)
                    .metadata(Map.of("sha256", sha256))
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromFile(file));
            log.info("[AWS S3] Upload complete - key: {}, Content-Type: {}", normalizedKey, contentType);
            return normalizedKey;

        } catch (Exception e) {
            log.error("[AWS S3] Upload failed - key: {}", key, e);
            throw new RuntimeException(ErrorCode.FILE_UPLOAD_FAILED.getMessage());
        }
    }

    @Override
    public File downloadFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            throw new RuntimeException("다운로드할 S3 URL이 없습니다.");
        }

        try {
            String pureKey = extractKey(fileUrl);

            File tempFile = File.createTempFile("s3_download_", ".mp3");
            Path tempFilePath = tempFile.toPath();

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(pureKey)
                    .build();

            s3Client.getObject(getObjectRequest, tempFilePath);
            return tempFile;

        } catch (Exception e) {
            log.error("[AWS S3] Download failed - URL: {}", fileUrl, e);
            throw new RuntimeException("S3 오디오 조각 다운로드 실패", e);
        }
    }

    @Override
    public String getPresignedUrl(String key, Duration durationUrl) {
        if (key == null || key.isEmpty()) return null;

        String pureKey = extractKey(key);

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

    @Override
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }

        try {
            String pureKey = extractKey(fileUrl);

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(pureKey)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("[AWS S3] Delete success: {}", pureKey);

        } catch (Exception e) {
            log.error("[AWS S3] Delete failed - URL: {}", fileUrl, e);
        }
    }

    private boolean objectExists(String key) {
        try {
            HeadObjectResponse response = s3Client.headObject(
                    HeadObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .build()
            );
            return response != null;
        } catch (S3Exception e) {
            if (isNotFound(e)) {
                return false;
            }
            throw e;
        }
    }

    private String findExistingKeyByContentDigest(String prefix, String sha256, String md5) {
        String continuationToken = null;

        do {
            ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .maxKeys(1000);

            if (continuationToken != null) {
                requestBuilder.continuationToken(continuationToken);
            }

            ListObjectsV2Response response = s3Client.listObjectsV2(requestBuilder.build());
            if (response == null || response.contents() == null || response.contents().isEmpty()) {
                return null;
            }

            for (S3Object object : response.contents()) {
                if (object == null || object.key() == null || object.key().endsWith("/")) {
                    continue;
                }

                // Fast path: ETag usually equals MD5 for single-part uploads.
                String normalizedETag = normalizeEtag(object.eTag());
                if (normalizedETag != null && normalizedETag.equalsIgnoreCase(md5)) {
                    return object.key();
                }

                String existingHash = fetchSha256Metadata(object.key());
                if (sha256.equals(existingHash)) {
                    return object.key();
                }
            }

            continuationToken = Boolean.TRUE.equals(response.isTruncated())
                    ? response.nextContinuationToken()
                    : null;

        } while (continuationToken != null && !continuationToken.isBlank());

        return null;
    }

    private String fetchSha256Metadata(String key) {
        try {
            HeadObjectResponse response = s3Client.headObject(
                    HeadObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .build()
            );
            if (response == null || response.metadata() == null || response.metadata().isEmpty()) {
                return null;
            }
            return response.metadata().get("sha256");
        } catch (S3Exception e) {
            if (isNotFound(e)) {
                return null;
            }
            throw e;
        }
    }

    private String calculateSha256(Path filePath) throws IOException {
        return calculateDigest(filePath, "SHA-256");
    }

    private String calculateMd5(Path filePath) throws IOException {
        return calculateDigest(filePath, "MD5");
    }

    private String calculateDigest(Path filePath, String algorithm) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] bytes = Files.readAllBytes(filePath);
            byte[] hashed = digest.digest(bytes);
            StringBuilder sb = new StringBuilder(hashed.length * 2);
            for (byte b : hashed) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(algorithm + " algorithm is not available", e);
        }
    }

    private String extractPrefix(String key) {
        if (key == null || key.isBlank()) {
            return "";
        }
        int idx = key.lastIndexOf('/');
        if (idx < 0) {
            return "";
        }
        return key.substring(0, idx + 1);
    }

    private String extractKey(String keyOrUrl) {
        if (keyOrUrl == null) {
            return null;
        }
        return keyOrUrl.contains(".com/") ? keyOrUrl.split(".com/")[1] : keyOrUrl;
    }

    private boolean isNotFound(S3Exception e) {
        if (e == null) {
            return false;
        }
        if (e.statusCode() == 404) {
            return true;
        }
        return e.awsErrorDetails() != null
                && "NotFound".equalsIgnoreCase(e.awsErrorDetails().errorCode());
    }

    private String normalizeEtag(String etag) {
        if (etag == null || etag.isBlank()) {
            return null;
        }
        return etag.replace("\"", "").trim();
    }
}


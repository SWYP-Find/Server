package com.swyp.picke.global.infra.s3.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class S3PresignedUrlService {

    private final S3Presigner s3Presigner;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;

    @Value("${app.s3.presigned-url.expiration-hours:6}")
    private int expirationHours;

    public String generatePresignedUrl(String s3KeyOrUrl) {
        if (s3KeyOrUrl == null || s3KeyOrUrl.isBlank()) {
            return null;
        }

        String key = extractKey(s3KeyOrUrl);

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(expirationHours))
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    public <K> Map<K, String> generatePresignedUrls(Map<K, String> keyMap) {
        if (keyMap == null || keyMap.isEmpty()) {
            return keyMap;
        }
        return keyMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> generatePresignedUrl(e.getValue())
                ));
    }

    /**
     * DB에는 보통 순수 key만 저장되지만, AWS S3를 쓰던 시절 저장된 레거시 데이터는
     * 전체 URL(https://{bucket}.s3.{region}.amazonaws.com/{key})일 수 있어 하위 호환을 위해 파싱한다.
     * 스토리지 제공자(AWS/Railway 등)에 무관하게 동작하도록 호스트가 아닌 경로 기준으로 key를 뽑는다.
     */
    private String extractKey(String input) {
        if (!input.startsWith("http://") && !input.startsWith("https://")) {
            return input;
        }
        String path = URI.create(input).getPath();
        return path.startsWith("/") ? path.substring(1) : path;
    }
}

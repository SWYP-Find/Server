package com.swyp.app.global.infra.s3.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

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

    private String extractKey(String input) {
        if (input.startsWith("https://") && input.contains(".s3.") && input.contains(".amazonaws.com/")) {
            int idx = input.indexOf(".amazonaws.com/") + ".amazonaws.com/".length();
            return input.substring(idx);
        }
        return input;
    }
}

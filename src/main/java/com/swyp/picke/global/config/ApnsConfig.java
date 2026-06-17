package com.swyp.picke.global.config;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.ApnsClientBuilder;
import com.eatthepath.pushy.apns.auth.ApnsSigningKey;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 다이렉트 APNs(Apple Push Notification service) 클라이언트 설정.
 * apns.enabled=true 일 때만 빈을 생성하며, 비활성화 시 ApnsPushService가 발송을 건너뛴다.
 * 컨테이너 배포 환경(Railway 등)은 영구 파일시스템이 없으므로,
 * S3에 업로드해둔 .p8 키(APNS_CREDENTIALS_S3_KEY)를 임시 파일로 받아 사용하고,
 * 없으면 로컬 개발 환경의 파일 경로(APNS_CREDENTIALS_PATH)를 사용한다.
 */
@Configuration
@ConditionalOnProperty(prefix = "apns", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class ApnsConfig {

    private final S3Client s3Client;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;

    @Value("${apns.credentials.s3-key:}")
    private String credentialsS3Key;

    @Value("${apns.credentials.location:}")
    private String credentialsLocation;

    @Value("${apns.key-id}")
    private String keyId;

    @Value("${apns.team-id}")
    private String teamId;

    @Value("${apns.production}")
    private boolean production;

    @Bean(destroyMethod = "close")
    public ApnsClient apnsClient() throws Exception {
        ApnsSigningKey signingKey = ApnsSigningKey.loadFromPkcs8File(
                resolveCredentialsFile(), teamId, keyId);

        return new ApnsClientBuilder()
                .setApnsServer(production
                        ? ApnsClientBuilder.PRODUCTION_APNS_HOST
                        : ApnsClientBuilder.DEVELOPMENT_APNS_HOST)
                .setSigningKey(signingKey)
                .build();
    }

    private File resolveCredentialsFile() throws Exception {
        if (!credentialsS3Key.isBlank()) {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(credentialsS3Key)
                    .build();

            Path tempFile = Files.createTempFile("apns-key", ".p8");
            Files.delete(tempFile);
            tempFile.toFile().deleteOnExit();
            s3Client.getObject(request, tempFile);
            return tempFile.toFile();
        }
        return new File(credentialsLocation);
    }
}

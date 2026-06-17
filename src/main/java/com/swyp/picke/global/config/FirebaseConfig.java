package com.swyp.picke.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 컨테이너 배포 환경(Railway 등)은 영구 파일시스템이 없으므로,
 * S3에 업로드해둔 서비스 계정 JSON(FCM_CREDENTIALS_S3_KEY)을 우선 사용하고,
 * 없으면 로컬 개발 환경의 파일 경로(FCM_CREDENTIALS_PATH)를 사용한다.
 */
@Configuration
@RequiredArgsConstructor
public class FirebaseConfig {

    private final S3Client s3Client;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;

    @Value("${firebase.credentials.s3-key:}")
    private String credentialsS3Key;

    @Value("${firebase.credentials.location:}")
    private String credentialsLocation;

    @Bean
    public FirebaseMessaging firebaseMessaging() throws IOException {
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(loadCredentials()))
                .build();

        FirebaseApp firebaseApp = FirebaseApp.getApps().isEmpty()
                ? FirebaseApp.initializeApp(options)
                : FirebaseApp.getInstance();

        return FirebaseMessaging.getInstance(firebaseApp);
    }

    private InputStream loadCredentials() throws IOException {
        if (!credentialsS3Key.isBlank()) {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(credentialsS3Key)
                    .build();
            return s3Client.getObject(request);
        }
        return new FileInputStream(credentialsLocation);
    }
}

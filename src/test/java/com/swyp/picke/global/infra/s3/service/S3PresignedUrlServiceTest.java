package com.swyp.picke.global.infra.s3.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static org.assertj.core.api.Assertions.assertThat;

class S3PresignedUrlServiceTest {

    private final S3Presigner presigner = S3Presigner.builder()
            .region(Region.AP_NORTHEAST_2)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
            .build();

    @AfterEach
    void tearDown() {
        presigner.close();
    }

    @Test
    @DisplayName("presigned URL 에 저장소 응답 캐시 헤더를 싣는다")
    void presignedUrl_hasResponseCacheControl() {
        S3PresignedUrlService service = new S3PresignedUrlService(presigner);
        ReflectionTestUtils.setField(service, "bucketName", "bucket");
        ReflectionTestUtils.setField(service, "expirationHours", 6);
        ReflectionTestUtils.setField(service, "objectMaxAgeSeconds", 86400L);

        String url = service.generatePresignedUrl("images/philosophers/kant.png");

        assertThat(url).contains("images/philosophers/kant.png")
                .contains("response-cache-control=public%2C%20max-age%3D86400");
    }
}

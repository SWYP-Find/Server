package com.swyp.picke.global.config;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.ApnsClientBuilder;
import com.eatthepath.pushy.apns.auth.ApnsSigningKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

/**
 * 다이렉트 APNs(Apple Push Notification service) 클라이언트 설정.
 * apns.enabled=true 일 때만 빈을 생성하며, 비활성화 시 ApnsPushService가 발송을 건너뛴다.
 */
@Configuration
@ConditionalOnProperty(prefix = "apns", name = "enabled", havingValue = "true")
public class ApnsConfig {

    @Value("${apns.credentials.location}")
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
                new File(credentialsLocation), teamId, keyId);

        return new ApnsClientBuilder()
                .setApnsServer(production
                        ? ApnsClientBuilder.PRODUCTION_APNS_HOST
                        : ApnsClientBuilder.DEVELOPMENT_APNS_HOST)
                .setSigningKey(signingKey)
                .build();
    }
}

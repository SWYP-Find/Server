package com.swyp.picke.global.config;

import com.google.crypto.tink.apps.rewardedads.RewardedAdsVerifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdMobConfig {

    @Bean
    public RewardedAdsVerifier rewardedAdsVerifier() {
        try {
            return new RewardedAdsVerifier.Builder()
                    .setVerifyingPublicKeys("https://www.gstatic.com/admob/reward/verifier-keys.json")
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("AdMob Verifier 초기화 실패!", e);
        }
    }
}
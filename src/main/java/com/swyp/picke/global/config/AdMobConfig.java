package com.swyp.picke.global.config;

import com.google.crypto.tink.apps.rewardedads.RewardedAdsVerifier;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Getter
@Configuration
public class AdMobConfig {

    @Value("${admob.reward.unit-id.ios}")
    private String iosRewardUnitId;

    @Value("${admob.reward.unit-id.android}")
    private String androidRewardUnitId;

    public List<String> getAllowedUnitIds() {
        return List.of(iosRewardUnitId, androidRewardUnitId);
    }

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
package com.swyp.picke.domain.ad.scheduler;

import com.swyp.picke.domain.ad.service.AdpickCampaignSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 애드픽 캠페인 주기 동기화.
 * 애드픽 가이드가 최대 1분에 1회 이하 호출을 요구해 넉넉한 간격으로 돈다.
 * 동기화가 실패해도 이미 저장된 소재로 광고는 계속 나가야 하므로 예외를 흘리지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdpickCampaignSyncScheduler {

    private final AdpickCampaignSyncService adpickCampaignSyncService;

    @Scheduled(
            initialDelayString = "${picke.ad.adpick.sync-initial-delay-ms:60000}",
            fixedDelayString = "${picke.ad.adpick.sync-interval-ms:600000}")
    public void sync() {
        try {
            adpickCampaignSyncService.sync();
        } catch (Exception e) {
            log.warn("[AdpickSync] 캠페인 동기화 실패: {}", e.getMessage());
        }
    }
}

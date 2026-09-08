package com.swyp.picke.domain.ad.service;

import com.swyp.picke.domain.ad.client.AdpickCampaignClient;
import com.swyp.picke.domain.ad.client.AdpickCampaignResponse;
import com.swyp.picke.domain.ad.entity.AdCreative;
import com.swyp.picke.domain.ad.enums.AdNetwork;
import com.swyp.picke.domain.ad.enums.AdSlotCode;
import com.swyp.picke.domain.ad.enums.AdSource;
import com.swyp.picke.domain.ad.enums.AdStatus;
import com.swyp.picke.domain.ad.enums.AdTargetOs;
import com.swyp.picke.domain.ad.repository.AdCreativeRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 애드픽 캠페인을 소재로 옮겨 담는다.
 * 수동 등록 소재와 같은 테이블을 쓰므로 로테이션·노출 집계·클릭 추적 경로를 그대로 탄다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdpickCampaignSyncService {

    private static final int SUBTITLE_MAX_LENGTH = 200;
    private static final int TITLE_MAX_LENGTH = 100;

    private final AdpickCampaignClient adpickCampaignClient;
    private final AdCreativeRepository adCreativeRepository;
    private final AdCreativeCodeGenerator adCreativeCodeGenerator;

    @Value("${picke.ad.adpick.slot:BATTLE_RESULT_BOTTOM}")
    private AdSlotCode slot;

    @Value("${picke.ad.adpick.cta-text:설치하고 받기}")
    private String ctaText;

    @Transactional
    public int sync() {
        if (!adpickCampaignClient.isConfigured()) {
            log.info("[AdpickSync] affId 미설정으로 동기화를 건너뛴다.");
            return 0;
        }

        List<AdpickCampaignResponse> campaigns = adpickCampaignClient.fetchCampaigns().stream()
                .filter(AdpickCampaignResponse::isRenderable)
                .toList();

        Map<String, AdCreative> existing = adCreativeRepository.findAllBySource(AdSource.ADPICK_API).stream()
                .collect(Collectors.toMap(AdCreative::getExternalId, Function.identity(), (a, b) -> a));

        Set<String> seen = new HashSet<>();
        for (AdpickCampaignResponse campaign : campaigns) {
            seen.add(campaign.offerId());
            upsert(existing.get(campaign.offerId()), campaign);
        }

        retire(existing, seen);
        log.info("[AdpickSync] 캠페인 {}건 동기화 완료", campaigns.size());
        return campaigns.size();
    }

    private void upsert(AdCreative found, AdpickCampaignResponse campaign) {
        if (found != null) {
            found.syncFromAdpick(
                    truncate(campaign.appTitle(), TITLE_MAX_LENGTH),
                    subtitleOf(campaign),
                    campaign.iconUrl(),
                    ctaText,
                    campaign.trackingLink(),
                    AdTargetOs.fromAdpick(campaign.os()),
                    slot,
                    campaign.hasRemaining());
            return;
        }

        adCreativeRepository.save(AdCreative.builder()
                .code(adCreativeCodeGenerator.generate())
                .network(AdNetwork.ADPICK)
                .slot(slot)
                .title(truncate(campaign.appTitle(), TITLE_MAX_LENGTH))
                .subtitle(subtitleOf(campaign))
                .imageUrl(campaign.iconUrl())
                .ctaText(ctaText)
                .landingUrl(campaign.trackingLink())
                .status(campaign.hasRemaining() ? AdStatus.ACTIVE : AdStatus.DRAFT)
                .weight(1)
                .source(AdSource.ADPICK_API)
                .externalId(campaign.offerId())
                .targetOs(AdTargetOs.fromAdpick(campaign.os()))
                .build());
    }

    /**
     * 피드에서 사라진 캠페인은 지우지 않고 내린다.
     * 이미 쌓인 노출·클릭 집계가 어느 소재의 것인지 계속 읽을 수 있어야 한다.
     */
    private void retire(Map<String, AdCreative> existing, Set<String> seen) {
        existing.forEach((externalId, creative) -> {
            if (!seen.contains(externalId) && creative.getStatus() == AdStatus.ACTIVE) {
                creative.changeStatus(AdStatus.DRAFT);
            }
        });
    }

    private String subtitleOf(AdpickCampaignResponse campaign) {
        String source = campaign.headline() != null && !campaign.headline().isBlank()
                ? campaign.headline()
                : campaign.promoText();
        if (source == null || source.isBlank()) {
            return null;
        }
        return truncate(source.replaceAll("\\s+", " ").trim(), SUBTITLE_MAX_LENGTH);
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() > max ? value.substring(0, max) : value;
    }
}

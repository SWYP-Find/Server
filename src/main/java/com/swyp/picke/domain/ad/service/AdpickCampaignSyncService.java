package com.swyp.picke.domain.ad.service;

import com.swyp.picke.domain.ad.client.AdpickCampaignClient;
import com.swyp.picke.domain.ad.client.AdpickCampaignResponse;
import com.swyp.picke.domain.ad.client.AdpickShoppingClient;
import com.swyp.picke.domain.ad.client.AdpickShoppingResponse;
import com.swyp.picke.domain.ad.entity.AdCreative;
import com.swyp.picke.domain.ad.enums.AdNetwork;
import com.swyp.picke.domain.ad.enums.AdSlotCode;
import com.swyp.picke.domain.ad.enums.AdSource;
import com.swyp.picke.domain.ad.enums.AdStatus;
import com.swyp.picke.domain.ad.enums.AdTargetOs;
import com.swyp.picke.domain.ad.repository.AdCreativeRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

    private static final String SHOPPING_ID_PREFIX = "sh";
    private static final int SHOPPING_ID_LENGTH = 10;

    private final AdpickCampaignClient adpickCampaignClient;
    private final AdpickShoppingClient adpickShoppingClient;
    private final AdCreativeRepository adCreativeRepository;
    private final AdCreativeCodeGenerator adCreativeCodeGenerator;

    @Value("${picke.ad.adpick.slot:BATTLE_RESULT_BOTTOM}")
    private AdSlotCode slot;

    @Value("${picke.ad.adpick.cta-text:설치하고 받기}")
    private String ctaText;

    /**
     * 로테이션 가중치. 쇼핑 상품을 앱 설치형보다 자주 띄우려고 나눠 둔다.
     * 앱 캠페인은 7건뿐이라 같은 가중치면 지면마다 같은 소재가 반복해서 뽑힌다.
     */
    @Value("${picke.ad.adpick.weight:1}")
    private int campaignWeight;

    @Value("${picke.ad.adpick.shopping-weight:3}")
    private int shoppingWeight;

    /**
     * 쇼핑·핫딜 상품을 흩어 놓을 지면.
     * 앱 캠페인은 7건뿐이라 한 지면밖에 못 채운다. 상품 60건을 나눠 담아 나머지 지면을 채운다.
     */
    @Value("${picke.ad.adpick.shopping-slots:HOME_FEED,CHAT_ROOM_INLINE,ATTENDANCE_COMPLETE,PROFILE_BOTTOM,BATTLE_RESULT_BOTTOM}")
    private List<AdSlotCode> shoppingSlots;

    @Value("${picke.ad.adpick.shopping-cta-text:구매하러 가기}")
    private String shoppingCtaText;

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

        int products = syncShopping(existing, seen);

        retire(existing, seen);
        log.info("[AdpickSync] 캠페인 {}건, 쇼핑 {}건 동기화 완료", campaigns.size(), products);
        return campaigns.size() + products;
    }

    /**
     * 쇼핑·핫딜 상품을 소재로 옮겨 담는다.
     *
     * <p>앱 캠페인과 같은 {@code ADPICK_API} 소스를 쓰되 식별자에 접두사를 둬서 섞이지 않게 한다.
     * 소스를 새로 만들면 {@code ck_ad_creatives_source} 제약을 배포 때 사람이 직접 ALTER 해야 한다.
     * 상품 하나 늘리자고 그 절차를 얹지 않는다.
     *
     * <p>상품 하나를 지면 하나에만 두면 지면당 재고가 전체의 1/지면수로 쪼개진다.
     * 쇼핑 상품은 지면을 가릴 이유가 없으므로 모든 쇼핑 지면에 각각 등록해 지면마다 전량을 쓴다.
     * 노출·클릭 집계는 소재 단위라, 지면별로 따로 세려면 소재도 지면별로 나뉘어 있어야 한다.
     */
    private int syncShopping(Map<String, AdCreative> existing, Set<String> seen) {
        if (!adpickShoppingClient.isConfigured() || shoppingSlots.isEmpty()) {
            return 0;
        }

        List<AdpickShoppingResponse> products = adpickShoppingClient.fetchProducts().stream()
                .filter(AdpickShoppingResponse::isRenderable)
                .toList();

        Set<String> buyUrls = new HashSet<>();
        int synced = 0;
        for (AdpickShoppingResponse product : products) {
            if (!buyUrls.add(product.buyUrl())) {
                // 쇼핑과 핫딜에 같은 상품이 함께 실릴 수 있다. 먼저 담은 쪽만 남긴다.
                continue;
            }
            for (AdSlotCode target : shoppingSlots) {
                String externalId = shoppingIdOf(product.buyUrl(), target);
                seen.add(externalId);
                upsertShopping(existing.get(externalId), externalId, target, product);
                synced++;
            }
        }
        return synced;
    }

    private void upsertShopping(
            AdCreative found, String externalId, AdSlotCode target, AdpickShoppingResponse product) {
        if (found != null) {
            found.syncFromAdpick(
                    truncate(product.productName(), TITLE_MAX_LENGTH),
                    truncate(product.subtitle(), SUBTITLE_MAX_LENGTH),
                    product.photo(),
                    shoppingCtaText,
                    product.buyUrl(),
                    AdTargetOs.ALL,
                    target,
                    shoppingWeight,
                    true);
            return;
        }

        adCreativeRepository.save(AdCreative.builder()
                .code(adCreativeCodeGenerator.generate())
                .network(AdNetwork.ADPICK)
                .slot(target)
                .title(truncate(product.productName(), TITLE_MAX_LENGTH))
                .subtitle(truncate(product.subtitle(), SUBTITLE_MAX_LENGTH))
                .imageUrl(product.photo())
                .ctaText(shoppingCtaText)
                .landingUrl(product.buyUrl())
                .status(AdStatus.ACTIVE)
                .weight(shoppingWeight)
                .source(AdSource.ADPICK_API)
                .externalId(externalId)
                .targetOs(AdTargetOs.ALL)
                .build());
    }

    /**
     * 상품에는 애드픽이 주는 코드가 없어 구매 링크에서 식별자를 만든다.
     * 링크가 그대로면 같은 소재로 갱신되고, 바뀌면 새 소재가 된다.
     *
     * <p>같은 상품이 지면마다 따로 등록되므로 지면까지 해시에 넣는다.
     * 길이와 모양({@code sh} + 16진수 10자)은 그대로 둔다. 조회 쪽이 이 형태로 쇼핑 소재를 가려낸다.
     */
    private String shoppingIdOf(String buyUrl, AdSlotCode slot) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((buyUrl + "|" + slot.name()).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(SHOPPING_ID_PREFIX);
            for (int i = 0; hex.length() < SHOPPING_ID_PREFIX.length() + SHOPPING_ID_LENGTH; i++) {
                hex.append(String.format("%02x", digest[i]));
            }
            return hex.substring(0, SHOPPING_ID_PREFIX.length() + SHOPPING_ID_LENGTH);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 을 쓸 수 없다", e);
        }
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
                    campaignWeight,
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
                .weight(campaignWeight)
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

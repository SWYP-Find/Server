package com.swyp.picke.domain.ad.service;

import com.swyp.picke.domain.ad.dto.response.AdResponse;
import com.swyp.picke.domain.ad.entity.AdCreative;
import com.swyp.picke.domain.ad.enums.AdSlotCode;
import com.swyp.picke.domain.ad.enums.AdNetwork;
import com.swyp.picke.domain.ad.enums.AdSource;
import com.swyp.picke.domain.ad.enums.AdStatus;
import com.swyp.picke.domain.ad.enums.AdTargetOs;
import com.swyp.picke.domain.ad.repository.AdCreativeRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdQueryService {

    /** 노출 집계 버킷과 게재 기간은 KST 기준이다. 진입점의 기본 시간대 설정에 기대지 않는다. */
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final AdCreativeRepository adCreativeRepository;
    private final AdImpressionRecorder adImpressionRecorder;

    @Value("${picke.ad.base-url:https://ad.picke.store}")
    private String adBaseUrl;

    /**
     * 지면에 노출할 소재를 가중 로테이션으로 고른다. 게재 가능한 소재가 없으면 빈 목록을 준다.
     * 앱은 빈 목록을 받으면 지면 자체를 숨긴다. 광고가 없는 건 오류가 아니다.
     * 애드픽 앱 설치형 캠페인은 OS가 갈리므로, 요청 OS와 맞지 않는 소재는 제외한다.
     */
    @Transactional(readOnly = true)
    public List<AdResponse> findServableAds(AdSlotCode slot, AdTargetOs os, int size) {
        LocalDateTime now = LocalDateTime.now(KST);
        AdTargetOs requested = os != null ? os : AdTargetOs.ALL;

        List<AdCreative> candidates = adCreativeRepository.findAllBySlotAndStatus(slot, AdStatus.ACTIVE).stream()
                .filter(creative -> creative.isServable(now))
                .filter(this::isShoppingOrManualAd)
                .filter(creative -> creative.getTargetOs().matches(requested))
                .toList();

        return weightedSample(candidates, size).stream()
                .map(creative -> AdResponse.of(creative, buildClickUrl(creative)))
                .toList();
    }

    /**
     * ad.picke.store 루트 공개 지면에 나열할 소재. 매체 심사에서 실제 콘텐츠를 확인하므로
     * 로테이션 없이 게재 가능한 소재를 모두 보여준다.
     */
    @Transactional(readOnly = true)
    public List<AdResponse> findLandingAds() {
        LocalDateTime now = LocalDateTime.now(KST);

        return adCreativeRepository.findAllByStatusOrderByIdDesc(AdStatus.ACTIVE).stream()
                .filter(creative -> creative.isServable(now))
                .filter(this::isShoppingOrManualAd)
                .map(creative -> AdResponse.of(creative, buildClickUrl(creative)))
                .toList();
    }

    /**
     * 조회 시점이 아니라 앱이 실제로 화면에 그린 시점에 호출된다.
     * 조회를 노출로 세면 CTR이 실제보다 낮게 왜곡되기 때문이다.
     *
     * <p>여기에 트랜잭션을 걸지 않는다. 삽입이 제약 위반으로 실패했을 때의 되돌리기가
     * 실패한 트랜잭션 밖에서 일어나야 하고, 한 소재의 집계 실패가 나머지 소재까지 되돌리면 안 된다.
     */
    public void recordImpressions(List<String> codes) {
        LocalDate today = LocalDate.now(KST);

        findImpressionTargets(codes).forEach(target -> increaseImpression(target, today));
    }

    private List<ImpressionTarget> findImpressionTargets(List<String> codes) {
        return adCreativeRepository.findAllByCodeIn(codes).stream()
                .map(creative -> new ImpressionTarget(creative.getId(), creative.getSlot()))
                .toList();
    }

    private void increaseImpression(ImpressionTarget target, LocalDate today) {
        if (adImpressionRecorder.increment(target.creativeId(), target.slot(), today)) {
            return;
        }

        try {
            adImpressionRecorder.insert(target.creativeId(), target.slot(), today);
        } catch (DataIntegrityViolationException e) {
            // 같은 (소재, 지면, 날짜) 행을 다른 요청이 먼저 만든 경우다. 갱신으로 되돌린다.
            // 삽입이 독립 트랜잭션이라 여기서 도는 갱신은 정상 트랜잭션에서 실행된다.
            adImpressionRecorder.increment(target.creativeId(), target.slot(), today);
        }
    }

    /** 동기화된 애드픽 설치·가입 캠페인은 제외한다. 쇼핑 식별자는 수집 시 생성한 sh + 해시다. */
    private boolean isShoppingOrManualAd(AdCreative creative) {
        if (creative.getNetwork() != AdNetwork.ADPICK || creative.getSource() != AdSource.ADPICK_API) {
            return true;
        }
        return creative.getExternalId() != null && creative.getExternalId().matches("sh[0-9a-f]{10}");
    }

    private String buildClickUrl(AdCreative creative) {
        return adBaseUrl + "/c/" + creative.getCode();
    }

    private List<AdCreative> weightedSample(List<AdCreative> candidates, int size) {
        List<AdCreative> pool = new ArrayList<>(candidates);
        List<AdCreative> picked = new ArrayList<>();

        while (!pool.isEmpty() && picked.size() < size) {
            picked.add(pickOne(pool));
        }
        return picked;
    }

    /** 가중치에 비례해 하나를 뽑고 풀에서 제거한다. 같은 소재가 한 응답에 두 번 담기지 않게 한다. */
    private AdCreative pickOne(List<AdCreative> pool) {
        int totalWeight = pool.stream().mapToInt(creative -> Math.max(1, creative.getWeight())).sum();
        int threshold = ThreadLocalRandom.current().nextInt(totalWeight);

        int accumulated = 0;
        for (Iterator<AdCreative> iterator = pool.iterator(); iterator.hasNext(); ) {
            AdCreative creative = iterator.next();
            accumulated += Math.max(1, creative.getWeight());
            if (threshold < accumulated) {
                iterator.remove();
                return creative;
            }
        }
        return pool.remove(pool.size() - 1);
    }

    private record ImpressionTarget(Long creativeId, AdSlotCode slot) {
    }
}

package com.swyp.picke.domain.ad.service;

import com.swyp.picke.domain.ad.dto.response.AdResponse;
import com.swyp.picke.domain.ad.entity.AdCreative;
import com.swyp.picke.domain.ad.entity.AdImpressionDaily;
import com.swyp.picke.domain.ad.enums.AdSlotCode;
import com.swyp.picke.domain.ad.enums.AdStatus;
import com.swyp.picke.domain.ad.repository.AdCreativeRepository;
import com.swyp.picke.domain.ad.repository.AdImpressionDailyRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    private final AdCreativeRepository adCreativeRepository;
    private final AdImpressionDailyRepository adImpressionDailyRepository;

    @Value("${picke.ad.base-url:https://ad.picke.store}")
    private String adBaseUrl;

    /**
     * 지면에 노출할 소재를 가중 로테이션으로 고른다. 게재 가능한 소재가 없으면 빈 목록을 준다.
     * 앱은 빈 목록을 받으면 지면 자체를 숨긴다. 광고가 없는 건 오류가 아니다.
     */
    @Transactional(readOnly = true)
    public List<AdResponse> findServableAds(AdSlotCode slot, int size) {
        LocalDateTime now = LocalDateTime.now();

        List<AdCreative> candidates = adCreativeRepository.findAllBySlotAndStatus(slot, AdStatus.ACTIVE).stream()
                .filter(creative -> creative.isServable(now))
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
        LocalDateTime now = LocalDateTime.now();

        return adCreativeRepository.findAllByStatusOrderByIdDesc(AdStatus.ACTIVE).stream()
                .filter(creative -> creative.isServable(now))
                .map(creative -> AdResponse.of(creative, buildClickUrl(creative)))
                .toList();
    }

    /**
     * 조회 시점이 아니라 앱이 실제로 화면에 그린 시점에 호출된다.
     * 조회를 노출로 세면 CTR이 실제보다 낮게 왜곡되기 때문이다.
     */
    @Transactional
    public void recordImpressions(List<String> codes) {
        LocalDate today = LocalDate.now();

        List<ImpressionTarget> targets = adCreativeRepository.findAllByCodeIn(codes).stream()
                .map(creative -> new ImpressionTarget(creative.getId(), creative.getSlot()))
                .toList();

        targets.forEach(target -> increaseImpression(target, today));
    }

    private void increaseImpression(ImpressionTarget target, LocalDate today) {
        if (adImpressionDailyRepository.increment(target.creativeId(), target.slot(), today, 1L) > 0) {
            return;
        }

        try {
            adImpressionDailyRepository.save(AdImpressionDaily.builder()
                    .creativeId(target.creativeId())
                    .slot(target.slot())
                    .statDate(today)
                    .impressions(1L)
                    .build());
        } catch (DataIntegrityViolationException e) {
            // 같은 (소재, 지면, 날짜) 행을 다른 요청이 먼저 만든 경우다. 갱신으로 되돌린다.
            adImpressionDailyRepository.increment(target.creativeId(), target.slot(), today, 1L);
        }
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

package com.swyp.picke.domain.admin.service;

import com.swyp.picke.domain.ad.entity.AdCreative;
import com.swyp.picke.domain.ad.enums.AdNetwork;
import com.swyp.picke.domain.ad.enums.AdSlotCode;
import com.swyp.picke.domain.ad.enums.AdStatus;
import com.swyp.picke.domain.ad.repository.AdClickLogRepository;
import com.swyp.picke.domain.ad.repository.AdCreativeRepository;
import com.swyp.picke.domain.ad.enums.AdSource;
import com.swyp.picke.domain.ad.repository.AdImpressionDailyRepository;
import com.swyp.picke.domain.ad.service.AdCreativeCodeGenerator;
import com.swyp.picke.domain.admin.dto.ad.request.AdCreativeRequest;
import com.swyp.picke.domain.admin.dto.ad.response.AdClickLogResponse;
import com.swyp.picke.domain.admin.dto.ad.response.AdCreativeResponse;
import com.swyp.picke.domain.admin.dto.ad.response.AdStatsResponse;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import com.swyp.picke.global.common.response.PageResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class AdminAdService {

    private final AdCreativeRepository adCreativeRepository;
    private final AdClickLogRepository adClickLogRepository;
    private final AdImpressionDailyRepository adImpressionDailyRepository;
    private final AdCreativeCodeGenerator adCreativeCodeGenerator;

    @Value("${coupang.partners.id:}")
    private String coupangPartnersId;

    @Transactional
    public AdCreativeResponse create(AdCreativeRequest request) {
        validateCoupangOwnership(request);

        AdCreative creative = AdCreative.builder()
                .code(adCreativeCodeGenerator.generate())
                .network(request.network())
                .slot(request.slot())
                .title(request.title())
                .subtitle(request.subtitle())
                .imageUrl(request.imageUrl())
                .ctaText(request.ctaText())
                .landingUrl(request.landingUrl())
                .status(request.status())
                .weight(request.weight())
                .startsAt(request.startsAt())
                .endsAt(request.endsAt())
                .source(AdSource.MANUAL)
                .targetOs(request.targetOs())
                .build();

        return AdCreativeResponse.from(adCreativeRepository.save(creative));
    }

    @Transactional
    public AdCreativeResponse update(Long creativeId, AdCreativeRequest request) {
        validateCoupangOwnership(request);

        AdCreative creative = findById(creativeId);
        rejectIfManaged(creative);

        creative.update(
                request.network(),
                request.slot(),
                request.title(),
                request.subtitle(),
                request.imageUrl(),
                request.ctaText(),
                request.landingUrl(),
                request.status(),
                request.weight(),
                request.startsAt(),
                request.endsAt(),
                request.targetOs()
        );

        return AdCreativeResponse.from(creative);
    }

    @Transactional
    public void delete(Long creativeId) {
        AdCreative creative = findById(creativeId);
        rejectIfManaged(creative);
        adCreativeRepository.delete(creative);
    }

    /**
     * 동기화 소재도 끌 수는 있어야 한다.
     * PAUSED 는 동기화가 되돌리지 않으므로 어드민의 끄기 스위치가 된다.
     */
    @Transactional
    public AdCreativeResponse changeStatus(Long creativeId, AdStatus status) {
        AdCreative creative = findById(creativeId);
        creative.changeStatus(status);
        return AdCreativeResponse.from(creative);
    }

    @Transactional(readOnly = true)
    public List<AdCreativeResponse> findAll(AdNetwork network, AdSlotCode slot, AdStatus status) {
        return adCreativeRepository.search(network, slot, status).stream()
                .map(AdCreativeResponse::from)
                .toList();
    }

    /**
     * 소재별 노출/클릭/CTR. 우리 DB 기준 수치이므로 제휴사 정산 리포트와 대조하는 용도다.
     */
    @Transactional(readOnly = true)
    public List<AdStatsResponse> findStats(LocalDate from, LocalDate to) {
        Map<Long, Long> impressions = adImpressionDailyRepository.sumByCreativeBetween(from, to).stream()
                .collect(Collectors.toMap(
                        AdImpressionDailyRepository.CreativeCount::getCreativeId,
                        AdImpressionDailyRepository.CreativeCount::getTotal));

        Map<Long, Long> clicks = adClickLogRepository
                .countByCreativeBetween(from.atStartOfDay(), to.plusDays(1).atStartOfDay()).stream()
                .collect(Collectors.toMap(
                        AdClickLogRepository.CreativeCount::getCreativeId,
                        AdClickLogRepository.CreativeCount::getTotal));

        return adCreativeRepository.findAllByOrderByIdDesc().stream()
                .map(creative -> AdStatsResponse.of(
                        creative.getId(),
                        creative.getCode(),
                        creative.getNetwork(),
                        creative.getSlot(),
                        creative.getTitle(),
                        impressions.getOrDefault(creative.getId(), 0L),
                        clicks.getOrDefault(creative.getId(), 0L)))
                .toList();
    }

    /**
     * 클릭 내역 목록. 어드민에서 "지금 광고가 실제로 눌리고 있는지"를 바로 확인하는 용도다.
     */
    @Transactional(readOnly = true)
    public PageResponse<AdClickLogResponse> findClickLogs(LocalDate from, LocalDate to, int page, int size) {
        return PageResponse.of(adClickLogRepository.findClickLogs(
                from.atStartOfDay(),
                to.plusDays(1).atStartOfDay(),
                PageRequest.of(Math.max(0, page - 1), size)));
    }

    /**
     * 남의 파트너스 링크를 잘못 붙여넣으면 우리가 광고를 싣고 수수료는 남이 받는다.
     * 다만 link.coupang.com 단축 링크에는 lptag가 드러나지 않으므로, 파라미터가 있을 때만 대조한다.
     * 없다고 막으면 정상적인 단축 링크를 쓸 수 없다.
     */
    private void validateCoupangOwnership(AdCreativeRequest request) {
        if (request.network() != AdNetwork.COUPANG || !StringUtils.hasText(coupangPartnersId)) {
            return;
        }

        String lptag = UriComponentsBuilder.fromUriString(request.landingUrl())
                .build()
                .getQueryParams()
                .getFirst("lptag");

        if (lptag != null && !coupangPartnersId.equals(lptag)) {
            throw new CustomException(ErrorCode.AD_COUPANG_PARTNER_MISMATCH);
        }
    }

    private AdCreative findById(Long creativeId) {
        return adCreativeRepository.findById(creativeId)
                .orElseThrow(() -> new CustomException(ErrorCode.AD_CREATIVE_NOT_FOUND));
    }

    /** 애드픽 동기화가 내용을 덮어쓰므로 어드민이 고치거나 지워도 다음 회차에 되돌아간다. */
    private void rejectIfManaged(AdCreative creative) {
        if (creative.isManaged()) {
            throw new CustomException(ErrorCode.AD_CREATIVE_MANAGED);
        }
    }
}

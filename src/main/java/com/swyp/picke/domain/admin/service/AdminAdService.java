package com.swyp.picke.domain.admin.service;

import com.swyp.picke.domain.ad.entity.AdCreative;
import com.swyp.picke.domain.ad.enums.AdNetwork;
import com.swyp.picke.domain.ad.enums.AdSlotCode;
import com.swyp.picke.domain.ad.enums.AdStatus;
import com.swyp.picke.domain.ad.repository.AdClickLogRepository;
import com.swyp.picke.domain.ad.repository.AdCreativeRepository;
import com.swyp.picke.domain.ad.repository.AdImpressionDailyRepository;
import com.swyp.picke.domain.admin.dto.ad.request.AdCreativeRequest;
import com.swyp.picke.domain.admin.dto.ad.response.AdClickLogResponse;
import com.swyp.picke.domain.admin.dto.ad.response.AdCreativeResponse;
import com.swyp.picke.domain.admin.dto.ad.response.AdStatsResponse;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import com.swyp.picke.global.common.response.PageResponse;
import java.security.SecureRandom;
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

    private static final String CODE_ALPHABET = "abcdefghijkmnpqrstuvwxyz23456789";
    private static final int CODE_LENGTH = 8;
    private static final int CODE_MAX_ATTEMPTS = 10;

    private final AdCreativeRepository adCreativeRepository;
    private final AdClickLogRepository adClickLogRepository;
    private final AdImpressionDailyRepository adImpressionDailyRepository;
    private final SecureRandom random = new SecureRandom();

    @Value("${coupang.partners.id:}")
    private String coupangPartnersId;

    @Transactional
    public AdCreativeResponse create(AdCreativeRequest request) {
        validateCoupangOwnership(request);

        AdCreative creative = AdCreative.builder()
                .code(generateUniqueCode())
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
                .build();

        return AdCreativeResponse.from(adCreativeRepository.save(creative));
    }

    @Transactional
    public AdCreativeResponse update(Long creativeId, AdCreativeRequest request) {
        validateCoupangOwnership(request);

        AdCreative creative = findById(creativeId);

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
                request.endsAt()
        );

        return AdCreativeResponse.from(creative);
    }

    @Transactional
    public void delete(Long creativeId) {
        adCreativeRepository.delete(findById(creativeId));
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
     *
     * <p>다만 link.coupang.com 단축 링크에는 lptag가 드러나지 않으므로, 파라미터가 있을 때만 대조한다.
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

    /** 헷갈리기 쉬운 글자(l, o, 0, 1)를 뺀 알파벳으로 코드를 만든다. 어드민이 눈으로 옮겨 적는 일이 있다. */
    private String generateUniqueCode() {
        for (int attempt = 0; attempt < CODE_MAX_ATTEMPTS; attempt++) {
            String code = randomCode();
            if (!adCreativeRepository.existsByCode(code)) {
                return code;
            }
        }
        throw new CustomException(ErrorCode.AD_CODE_GENERATION_FAILED);
    }

    private String randomCode() {
        return random.ints(CODE_LENGTH, 0, CODE_ALPHABET.length())
                .mapToObj(index -> String.valueOf(CODE_ALPHABET.charAt(index)))
                .collect(Collectors.joining());
    }
}

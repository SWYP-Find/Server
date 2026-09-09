package com.swyp.picke.domain.ad.service;

import com.swyp.picke.domain.ad.entity.AdClickLog;
import com.swyp.picke.domain.ad.enums.AdSlotCode;
import com.swyp.picke.domain.ad.link.AffiliateLinkResolver;
import com.swyp.picke.domain.ad.repository.AdClickLogRepository;
import com.swyp.picke.domain.ad.repository.AdCreativeRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdClickService {

    private static final int USER_AGENT_MAX_LENGTH = 500;

    /** 게재 기간 판단은 KST 기준이다. 진입점의 기본 시간대 설정에 기대지 않는다. */
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final AdCreativeRepository adCreativeRepository;
    private final AdClickLogRepository adClickLogRepository;
    private final AffiliateLinkResolver affiliateLinkResolver;

    /**
     * 클릭 코드로 최종 이동 대상을 찾는다. 코드가 없거나 게재 기간이 지났으면 비어 있는 값을 준다.
     */
    @Transactional(readOnly = true)
    public Optional<AdClickTarget> resolveTarget(String code) {
        return adCreativeRepository.findByCode(code)
                .filter(creative -> creative.isServable(LocalDateTime.now(KST)))
                .map(creative -> new AdClickTarget(
                        creative.getId(),
                        creative.getSlot(),
                        affiliateLinkResolver.resolve(creative)
                ));
    }

    /**
     * 클릭 적재가 리다이렉트를 붙잡으면 안 되므로 비동기로 둔다.
     * 적재에 실패해도 사용자는 정상적으로 제휴처로 이동해야 한다.
     */
    @Async
    @Transactional
    public void recordClick(AdClickTarget target, String clientIp, String userAgent) {
        try {
            adClickLogRepository.save(AdClickLog.builder()
                    .creativeId(target.creativeId())
                    .slot(target.slot())
                    .ipHash(hashIp(clientIp))
                    .userAgent(truncate(userAgent))
                    .build());
        } catch (Exception e) {
            log.warn("[AdClick] 클릭 적재 실패 creativeId={}: {}", target.creativeId(), e.getMessage());
        }
    }

    /** 원본 IP는 남기지 않는다. 중복 클릭 판별에 필요한 정도만 해시로 보관한다. */
    private String hashIp(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(clientIp.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    private String truncate(String userAgent) {
        if (userAgent == null) {
            return null;
        }
        return userAgent.length() > USER_AGENT_MAX_LENGTH
                ? userAgent.substring(0, USER_AGENT_MAX_LENGTH)
                : userAgent;
    }

    public record AdClickTarget(Long creativeId, AdSlotCode slot, String redirectUrl) {
    }
}

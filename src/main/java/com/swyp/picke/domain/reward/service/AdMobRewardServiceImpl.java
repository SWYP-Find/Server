package com.swyp.picke.domain.reward.service;

import com.google.crypto.tink.apps.rewardedads.RewardedAdsVerifier;
import com.swyp.picke.domain.reward.dto.request.AdMobRewardRequest;
import com.swyp.picke.domain.reward.entity.AdRewardHistory;
import com.swyp.picke.domain.reward.repository.AdRewardHistoryRepository;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.enums.CreditType;
import com.swyp.picke.domain.user.service.CreditService;
import com.swyp.picke.domain.user.service.UserService;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import com.swyp.picke.global.config.AdMobConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.GeneralSecurityException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdMobRewardServiceImpl implements AdMobRewardService {

    private final RewardedAdsVerifier rewardedAdsVerifier;
    private final AdRewardHistoryRepository adRewardHistoryRepository;
    private final UserService userService;
    private final CreditService creditService;
    private final AdMobConfig adMobConfig;

    @Override
    @Transactional
    public String processReward(AdMobRewardRequest request) {
        log.info("[AdMob] SSV 처리 시작: transaction_id={}, userTag={}, reward_amount={}, ad_unit={}",
                request.transaction_id(), request.getUserTag(), request.reward_amount(), request.ad_unit());

        // 1. ad_unit 유효성 검사
        if (!adMobConfig.getAllowedUnitIds().contains(request.ad_unit())) {
            log.warn("[AdMob] 허용되지 않은 ad_unit: {}", request.ad_unit());
            return "OK";
        }

        // 2. 서명 검증 (공식 파라미터 기반)
        if (!verifyAdMobSignature(request)) {
            log.warn("[AdMob] 서명 검증 실패: transaction_id={}", request.transaction_id());
            return "OK";
        }

        // 3. 중복 처리 방지
        if (adRewardHistoryRepository.existsByTransactionId(request.transaction_id())) {
            log.info("[AdMob] 이미 처리된 요청: transaction_id={}", request.transaction_id());
            return "Already Processed";
        }

        // 4. 유저 확인 (custom_data → user_id 순으로 조회)
        log.info("[AdMob] 유저 조회 시도: userTag={}", request.getUserTag());
        User user = userService.findByUserTag(request.getUserTag());
        log.info("[AdMob] 유저 확인 완료: userId={}", user.getId());

        // 5. 보상 이력 저장 후 즉시 id 확보 (saveAndFlush)
        AdRewardHistory history = AdRewardHistory.builder()
                .transactionId(request.transaction_id())
                .user(user)
                .rewardAmount(request.reward_amount())
                .rewardItem(request.getRewardType())
                .build();
        adRewardHistoryRepository.saveAndFlush(history);
        log.info("[AdMob] 보상 이력 저장 완료: historyId={}", history.getId());

        // 6. 크레딧 적립 (history.getId()를 referenceId로 사용해 unique 충돌 방지)
        // AdMob이 보낸 reward_amount는 콘솔 설정에 따라 달라질 수 있어 신뢰하지 않고, 정책상 고정값(FREE_CHARGE)만 지급한다
        creditService.addCredit(user.getId(), CreditType.FREE_CHARGE, CreditType.FREE_CHARGE.getDefaultAmount(), history.getId());
        log.info("[AdMob] 포인트 적립 완료: userId={}, amount={}, historyId={}",
                user.getId(), CreditType.FREE_CHARGE.getDefaultAmount(), history.getId());

        return "OK";
    }

    /**
     * // 6. 서명 검증 로직 수정
     * 구글 공식 문서의 파라미터 순서와 명칭(ad_unit 등)을 엄격히 준수해야 합니다.
     */
    private boolean verifyAdMobSignature(AdMobRewardRequest request) {
        try {
            // RewardedAdsVerifier.verify()는 내부에서 URI.getQuery()를 사용하므로 scheme을 포함한 완전한 URL이어야 함
            // key_id와 signature는 반드시 마지막 두 파라미터 순서로 위치해야 함 (Tink 라이브러리 스펙)
            StringBuilder sb = new StringBuilder("https://admob.google.com/reward?");
            if (request.ad_network() != null) sb.append("ad_network=").append(request.ad_network()).append("&");
            sb.append("ad_unit=").append(request.ad_unit()).append("&");
            if (request.custom_data() != null) sb.append("custom_data=").append(request.custom_data()).append("&");
            sb.append("reward_amount=").append(request.reward_amount()).append("&");
            sb.append("reward_item=").append(request.reward_item()).append("&");
            sb.append("timestamp=").append(request.timestamp()).append("&");
            sb.append("transaction_id=").append(request.transaction_id());
            if (request.user_id() != null) sb.append("&user_id=").append(request.user_id());
            sb.append("&key_id=").append(request.key_id());
            sb.append("&signature=").append(request.signature());

            String fullUrl = sb.toString();

            rewardedAdsVerifier.verify(fullUrl);
            return true;
        } catch (GeneralSecurityException e) {
            log.error("보상 서명 보안 에러: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("보상 검증 중 예상치 못한 에러: {}", e.getMessage());
            return false;
        }
    }
}
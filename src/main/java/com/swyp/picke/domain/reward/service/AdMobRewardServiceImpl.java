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
        creditService.addCredit(user.getId(), CreditType.FREE_CHARGE, request.reward_amount(), history.getId());
        log.info("[AdMob] 포인트 적립 완료: userId={}, amount={}, historyId={}",
                user.getId(), request.reward_amount(), history.getId());

        return "OK";
    }

    /**
     * // 6. 서명 검증 로직 수정
     * 구글 공식 문서의 파라미터 순서와 명칭(ad_unit 등)을 엄격히 준수해야 합니다.
     */
    private boolean verifyAdMobSignature(AdMobRewardRequest request) {
        try {
            // // 조립 시 signature와 key_id는 제외하고 나머지 8개 파라미터를 조립합니다.
            // // 순서: ad_network -> ad_unit -> custom_data -> reward_amount -> reward_item -> timestamp -> transaction_id -> user_id
            StringBuilder sb = new StringBuilder();
            if (request.ad_network() != null) sb.append("ad_network=").append(request.ad_network()).append("&");
            sb.append("ad_unit=").append(request.ad_unit()).append("&");
            if (request.custom_data() != null) sb.append("custom_data=").append(request.custom_data()).append("&");
            sb.append("reward_amount=").append(request.reward_amount()).append("&");
            sb.append("reward_item=").append(request.reward_item()).append("&");
            sb.append("timestamp=").append(request.timestamp()).append("&");
            sb.append("transaction_id=").append(request.transaction_id());
            if (request.user_id() != null) sb.append("&user_id=").append(request.user_id());

            String fullQueryString = sb.toString();

            // // Tink 라이브러리를 통해 signature와 key_id를 사용하여 검증
            rewardedAdsVerifier.verify(fullQueryString);
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
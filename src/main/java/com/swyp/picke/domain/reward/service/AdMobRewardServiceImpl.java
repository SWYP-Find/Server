package com.swyp.picke.domain.reward.service;

import com.google.crypto.tink.apps.rewardedads.RewardedAdsVerifier;
import com.swyp.picke.domain.reward.dto.request.AdMobRewardRequest;
import com.swyp.picke.domain.reward.entity.AdRewardHistory;
import com.swyp.picke.domain.reward.repository.AdRewardHistoryRepository;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.enums.CreditType;
import com.swyp.picke.domain.user.service.CreditService;
import com.swyp.picke.domain.user.service.UserService; // // 1. UserService 임포트
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
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
    private final UserService userService; // // 2. UserRepository 대신 UserService 사용 (태그 조회 로직 집중)
    private final CreditService creditService;

    @Override
    @Transactional
    public String processReward(AdMobRewardRequest request) {
        // 1. 서명 검증 (공식 파라미터 기반)
        /*if (!verifyAdMobSignature(request)) {
            log.warn("AdMob 서명 검증 실패: transaction_id={}", request.transaction_id());
            throw new CustomException(ErrorCode.REWARD_INVALID_SIGNATURE);
        }*/

        // 2. 중복 처리 방지
        if (adRewardHistoryRepository.existsByTransactionId(request.transaction_id())) {
            log.info("이미 처리된 광고 요청입니다: transaction_id={}", request.transaction_id());
            return "Already Processed";
        }

        // 3. 유저 확인 (UserTag를 이용해 UserService에서 실제 유저 확보)
        // request.getUserTag()는 custom_data 혹은 user_id를 반환합니다.
        User user = userService.findByUserTag(request.getUserTag());

        // 4. 보상 이력(AdRewardHistory) 저장
        AdRewardHistory history = AdRewardHistory.builder()
                .transactionId(request.transaction_id())
                .user(user)
                .rewardAmount(request.reward_amount())
                .rewardItem(request.getRewardType()) // // Enum 명칭 저장
                .build();
        adRewardHistoryRepository.save(history);

        // 5. 크레딧 적립
        Long refId = parseTransactionId(request.transaction_id());
        creditService.addCredit(user.getId(), CreditType.FREE_CHARGE, request.reward_amount(), refId);

        log.info("보상 지급 완료: userTag={}, userId={}, amount={}",
                 user.getUserTag(), user.getId(), request.reward_amount());
        return "OK";
    }

    private Long parseTransactionId(String transactionId) {
        try {
            return Long.parseLong(transactionId.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return (long) Math.abs(transactionId.hashCode());
        }
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
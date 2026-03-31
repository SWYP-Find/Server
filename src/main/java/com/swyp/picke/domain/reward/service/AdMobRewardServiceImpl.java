package com.swyp.picke.domain.reward.service;

import com.google.crypto.tink.apps.rewardedads.RewardedAdsVerifier;
import com.swyp.picke.domain.reward.dto.request.AdMobRewardRequest;
import com.swyp.picke.domain.reward.entity.AdRewardHistory;
import com.swyp.picke.domain.reward.repository.AdRewardHistoryRepository;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.enums.CreditType;
import com.swyp.picke.domain.user.repository.UserRepository;
import com.swyp.picke.domain.user.service.CreditService;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.GeneralSecurityException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdMobRewardServiceImpl implements AdMobRewardService {

    private final RewardedAdsVerifier rewardedAdsVerifier;
    private final AdRewardHistoryRepository adRewardHistoryRepository;
    private final UserRepository userRepository;
    private final CreditService creditService;

    @Override
    @Transactional
    public String processReward(AdMobRewardRequest request) {
        // 1. 서명 검증 (변경된 조립 로직 사용)
        if (!verifyAdMobSignature(request)) {
            log.warn("AdMob 서명 검증 실패: transaction_id={}", request.transaction_id());
            throw new CustomException(ErrorCode.REWARD_INVALID_SIGNATURE);
        }

        // 2. 중복 처리 방지
        if (adRewardHistoryRepository.existsByTransactionId(request.transaction_id())) {
            log.info("이미 처리된 광고 요청입니다: transaction_id={}", request.transaction_id());
            return "Already Processed";
        }

        // 3. 유저 확인
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.REWARD_INVALID_USER));

        // 4. 보상 이력 저장 (순서 최적화)
        AdRewardHistory history = AdRewardHistory.builder()
                .transactionId(request.transaction_id())
                .user(user)
                .rewardAmount(request.reward_amount())
                .rewardItem(request.getRewardType())
                .build();
        adRewardHistoryRepository.save(history);

        // // 5. 크레딧 적립
        Long refId = parseTransactionId(request.transaction_id());
        creditService.addCredit(user.getId(), CreditType.AD_REWARD, refId);

        log.info("보상 지급 완료: user={}, amount={}", user.getId(), request.reward_amount());
        return "OK";
    }

    private Long parseTransactionId(String transactionId) {
        try {
            return Long.parseLong(transactionId.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return (long) transactionId.hashCode();
        }
    }

    private boolean verifyAdMobSignature(AdMobRewardRequest request) {
        try {
            // 공식 가이드에 따른 쿼리 스트링 조립 (ad_unit_id -> ad_unit 등)
            // signature와 key_id는 verify 메서드 내부에서 파라미터로 처리되거나
            // 전체 URL에 포함되어 있어야 하므로, 여기서는 데이터 부분만 정확히 조립합니다.
            StringBuilder sb = new StringBuilder();
            sb.append("ad_network=").append(request.ad_network()).append("&");
            sb.append("ad_unit=").append(request.ad_unit()).append("&");
            sb.append("custom_data=").append(request.custom_data()).append("&");
            sb.append("reward_amount=").append(request.reward_amount()).append("&");
            sb.append("reward_item=").append(request.reward_item()).append("&");
            sb.append("timestamp=").append(request.timestamp()).append("&");
            sb.append("transaction_id=").append(request.transaction_id()).append("&");
            sb.append("user_id=").append(request.user_id());

            // Tink 라이브러리의 verify는 데이터 문자열 + "&signature=..." + "&key_id=..." 형태를 기대합니다.
            String fullData = sb.toString() + "&signature=" + request.signature() + "&key_id=" + request.key_id();

            rewardedAdsVerifier.verify(fullData);
            return true;
        } catch (GeneralSecurityException e) {
            log.error("AdMob 서명 검증 실패: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("AdMob 서명 검증 중 오류 발생: {}", e.getMessage());
            return false;
        }
    }
}
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
    private final CreditService creditService; // 1. CreditService 주입

    @Override
    @Transactional
    public String processReward(AdMobRewardRequest request) {
//        // 1. 서명 검증
//        if (!verifyAdMobSignature(request)) {
//            log.warn("AdMob 서명 검증 실패: transaction_id={}", request.transaction_id());
//            throw new CustomException(ErrorCode.REWARD_INVALID_SIGNATURE);
//        }
//
        // 2. 중복 처리 방지
        // 만약 여기서 true가 안 나온다면, DB에 transaction_id가 아직 안 쌓인 상태입니다.
        if (adRewardHistoryRepository.existsByTransactionId(request.transaction_id())) {
            log.info("이미 처리된 광고 요청입니다: transaction_id={}", request.transaction_id());
            return "Already Processed";
        }
//
//        // 3. 유저 확인
//        User user = userRepository.findById(request.getUserId())
//                .orElseThrow(() -> new CustomException(ErrorCode.REWARD_INVALID_USER));
//
//        // 4. 보상 이력(AdRewardHistory)을 먼저 저장
//        // 이력을 먼저 남겨야 다음 요청이 들어왔을 때 위 2번 로직에서 걸러집니다.
//        AdRewardHistory history = AdRewardHistory.builder()
//                .transactionId(request.transaction_id())
//                .user(user)
//                .rewardAmount(request.reward_amount())
//                .rewardItem(request.getRewardType())
//                .build();
//        adRewardHistoryRepository.save(history);
//
//        // 5. 크레딧 적립
//        Long refId = parseTransactionId(request.transaction_id());
//        creditService.addCredit(user.getId(), CreditType.AD_REWARD, refId);
//
//        log.info("보상 지급 완료: user={}, amount={}", user.getId(), request.reward_amount());
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
            String fullQueryString = String.format(
                    "ad_unit_id=%s&custom_data=%s&reward_amount=%d&reward_item=%s&timestamp=%d&transaction_id=%s&signature=%s&key_id=%s",
                    request.ad_unit_id(), request.custom_data(), request.reward_amount(),
                    request.reward_item(), request.timestamp(), request.transaction_id(),
                    request.signature(), request.key_id()
            );

            rewardedAdsVerifier.verify(fullQueryString);
            return true;
        } catch (GeneralSecurityException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
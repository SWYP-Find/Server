package com.swyp.picke.domain.reward.service;

import com.google.crypto.tink.apps.rewardedads.RewardedAdsVerifier;
import com.swyp.picke.domain.reward.dto.request.AdMobRewardRequest;
import com.swyp.picke.domain.reward.entity.AdRewardHistory;
import com.swyp.picke.domain.reward.repository.AdRewardHistoryRepository;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.repository.UserRepository;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import jakarta.transaction.Transactional;
import java.security.GeneralSecurityException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdMobRewardServiceImpl implements AdMobRewardService {

    private final RewardedAdsVerifier rewardedAdsVerifier;
    private final AdRewardHistoryRepository adRewardHistoryRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public String processReward(AdMobRewardRequest request) {
        // 2. 서명 검증 (구글이 보낸 진짜 신호인지 확인)
        if (!verifyAdMobSignature(request)) {
            log.warn("AdMob 서명 검증 실패: transaction_id={}", request.transaction_id());
            throw new CustomException(ErrorCode.REWARD_INVALID_SIGNATURE);
        }

        // 3. 중복 처리 방지 (멱등성 유지)
        if (adRewardHistoryRepository.existsByTransactionId(request.transaction_id())) {
            log.info("이미 처리된 광고 요청입니다: transaction_id={}", request.transaction_id());
            return "Already Processed";
        }

        // 4. 유저 존재 여부 확인 (DTO의 getUserId 활용)
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.REWARD_INVALID_USER));

        // 5. 보상 이력 저장 (영수증 남기기)
        AdRewardHistory history = AdRewardHistory.builder()
                .transactionId(request.transaction_id())
                .user(user)
                .rewardAmount(request.reward_amount())
                .rewardItem(request.getRewardType())
                .build();

        adRewardHistoryRepository.save(history);

        log.info("보상 지급 완료: user={}, amount={}", user.getId(), request.reward_amount());
        return "OK";
    }

    /**
     * Google Tink를 이용한 SSV 서명 검증 로직
     */
    private boolean verifyAdMobSignature(AdMobRewardRequest request) {
        try {
            // signature와 key_id까지 모두 포함된 전체 쿼리 스트링을 만듭니다.
            // (구글이 우리 서버에 쏜 URL의 뒷부분 전체라고 보시면 됩니다.)
            String fullQueryString = String.format(
                    "ad_unit_id=%s&custom_data=%s&reward_amount=%d&reward_item=%s&timestamp=%d&transaction_id=%s&signature=%s&key_id=%s",
                    request.ad_unit_id(), request.custom_data(), request.reward_amount(),
                    request.reward_item(), request.timestamp(), request.transaction_id(),
                    request.signature(), request.key_id()
            );

            rewardedAdsVerifier.verify(fullQueryString);
            return true;

        } catch (GeneralSecurityException e) {
            log.error("AdMob 서명 검증 실패: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("검증 중 알 수 없는 오류: {}", e.getMessage());
            return false;
        }
    }
}

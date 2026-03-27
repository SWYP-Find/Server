package com.swyp.app.domain.reward.service;

import com.swyp.app.domain.reward.dto.request.AdMobRewardRequest;
import com.swyp.app.domain.reward.entity.AdRewardHistory;
import com.swyp.app.domain.reward.repository.AdRewardHistoryRepository;
import com.swyp.app.domain.user.entity.User;
import com.swyp.app.domain.user.repository.UserRepository;
import com.swyp.app.global.common.exception.CustomException;
import com.swyp.app.global.common.exception.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdMobRewardServiceImpl implements AdMobRewardService {

    private final AdRewardHistoryRepository adRewardHistoryRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public String processReward(AdMobRewardRequest request) {
        // 1. [보안] 서명 검증 (명세서 REWARD_VERIFICATION_FAILED 반영)
        if (!verifyAdMobSignature(request)) {
            log.error("잘못된 AdMob 서명 요청: transaction_id={}", request.transaction_id());
            throw new CustomException(ErrorCode.REWARD_VERIFICATION_FAILED);
        }

        // 2. [중복 방지] 이미 처리된 트랜잭션인지 확인 (명세서 2.1 반영)
        // 💡 이미 처리된 경우 에러가 아니라 "Already Processed"를 반환해 구글 재시도를 막음
        if (adRewardHistoryRepository.existsByTransactionId(request.transaction_id())) {
            log.warn("이미 처리된 광고 시청 건입니다: {}", request.transaction_id());
            return "Already Processed";
        }

        // 3. [유저 식별] custom_data 조회 (명세서 REWARD_INVALID_USER 반영)
        Long userId = request.getUserId(); // DTO 내부 로직에서 CustomException 발생

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("존재하지 않는 유저 ID: {}", userId);
                    return new CustomException(ErrorCode.REWARD_INVALID_USER);
                });

        // 4. [보상 지급 및 이력 저장]
        // TODO: user.updatePoint(request.reward_amount());

        AdRewardHistory history = AdRewardHistory.builder()
                .user(user)
                .transactionId(request.transaction_id())
                .rewardAmount(request.reward_amount())
                .rewardType(request.getRewardType()) // DTO 내부 로직에서 CustomException 발생
                .build();

        adRewardHistoryRepository.save(history);

        log.info("광고 보상 지급 완료 - 유저: {}, 금액: {}, ID: {}",
                 user.getId(), request.reward_amount(), request.transaction_id());

        return "OK";
    }

    private boolean verifyAdMobSignature(AdMobRewardRequest request) {
        // TODO: 구글 Tink 라이브러리 연동 로직 구현
        return true;
    }
}

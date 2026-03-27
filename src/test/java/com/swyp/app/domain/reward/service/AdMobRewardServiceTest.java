package com.swyp.app.domain.reward.service;

import com.swyp.app.domain.reward.dto.request.AdMobRewardRequest;
import com.swyp.app.domain.reward.repository.AdRewardHistoryRepository;
import com.swyp.app.domain.user.entity.User;
import com.swyp.app.domain.user.repository.UserRepository;
import com.swyp.app.global.common.exception.CustomException;
import com.swyp.app.global.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdMobRewardServiceTest {

    @InjectMocks
    private AdMobRewardServiceImpl rewardService;

    @Mock
    private AdRewardHistoryRepository adRewardHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("1. 정상적인 광고 시청 시 보상 이력이 저장되고 OK를 반환한다.")
    void processReward_Success() {
        // given
        AdMobRewardRequest request = new AdMobRewardRequest(
                "ad-unit-123", "1", 100, "POINT", 123456789L,
                "unique-trans-id", "sig", "key"
        );
        User mockUser = mock(User.class);

        given(adRewardHistoryRepository.existsByTransactionId(request.transaction_id())).willReturn(false);
        given(userRepository.findById(1L)).willReturn(Optional.of(mockUser));

        // when
        String result = rewardService.processReward(request);

        // then
        assertThat(result).isEqualTo("OK");
        verify(adRewardHistoryRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("2. 이미 존재하는 트랜잭션 ID인 경우 Already Processed를 반환하고 저장을 생략한다.")
    void processReward_Duplicate() {
        // given
        String duplicateId = "duplicate-id";
        AdMobRewardRequest request = new AdMobRewardRequest(
                "ad-unit-123", "1", 100, "POINT", 123456789L,
                duplicateId, "sig", "key"
        );

        given(adRewardHistoryRepository.existsByTransactionId(duplicateId)).willReturn(true);

        // when
        String result = rewardService.processReward(request);

        // then
        assertThat(result).isEqualTo("Already Processed");
        verify(adRewardHistoryRepository, never()).save(any()); // 저장 로직이 호출되지 않아야 함
        verifyNoInteractions(userRepository); // 유저 조회조차 하지 않아야 효율적임
    }

    @Test
    @DisplayName("3. 존재하지 않는 유저 ID인 경우 REWARD_INVALID_USER 예외가 발생한다.")
    void processReward_UserNotFound() {
        // given
        AdMobRewardRequest request = new AdMobRewardRequest(
                "ad-unit-123", "999", 100, "POINT", 123456789L,
                "new-id", "sig", "key"
        );

        given(adRewardHistoryRepository.existsByTransactionId(any())).willReturn(false);
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> rewardService.processReward(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REWARD_INVALID_USER);
    }
}
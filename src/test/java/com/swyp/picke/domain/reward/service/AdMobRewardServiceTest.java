package com.swyp.picke.domain.reward.service;

import com.google.crypto.tink.apps.rewardedads.RewardedAdsVerifier;
import com.swyp.picke.domain.reward.dto.request.AdMobRewardRequest;
import com.swyp.picke.domain.reward.entity.AdRewardHistory;
import com.swyp.picke.domain.reward.repository.AdRewardHistoryRepository;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.repository.UserRepository;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.GeneralSecurityException;
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

    @Mock
    private RewardedAdsVerifier rewardedAdsVerifier;

    @Test
    @DisplayName("1. 정상적인 광고 시청 시 보상 이력이 저장되고 OK를 반환한다.")
    void processReward_Success() throws Exception {
        // given
        AdMobRewardRequest request = createSampleRequest("unique-id");
        User mockUser = mock(User.class);

        given(adRewardHistoryRepository.existsByTransactionId(request.transaction_id())).willReturn(false);
        doNothing().when(rewardedAdsVerifier).verify(anyString());
        given(userRepository.findById(1L)).willReturn(Optional.of(mockUser));

        // when
        String result = rewardService.processReward(request);

        // then
        assertThat(result).isEqualTo("OK");
        verify(adRewardHistoryRepository, times(1)).save(any(AdRewardHistory.class));
    }

    @Test
    @DisplayName("2. 서명 검증에 실패하면 REWARD_INVALID_SIGNATURE 예외가 발생한다.")
    void processReward_InvalidSignature() throws Exception {
        // given
        AdMobRewardRequest request = createSampleRequest("trans-id");

        // // 1. 불필요한 existsByTransactionId 스터빙 제거 (만약 서비스에서 검증을 먼저 한다면 호출 안 될 수 있음)
        // // 만약 호출이 반드시 일어난다면 아래 주석을 풀고 사용하세요.
        lenient().when(adRewardHistoryRepository.existsByTransactionId(anyString())).thenReturn(false);

        // // 2. 서명 검증 실패 시뮬레이션
        doThrow(new GeneralSecurityException("Invalid signature"))
                .when(rewardedAdsVerifier).verify(anyString());

        // when & then
        assertThatThrownBy(() -> rewardService.processReward(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REWARD_INVALID_SIGNATURE);

        verify(adRewardHistoryRepository, never()).save(any());
    }

    private AdMobRewardRequest createSampleRequest(String transId) {
        return new AdMobRewardRequest(
                "ad-unit-123", "1", 100, "POINT", 123456789L,
                transId, "sig-123", "key-123"
        );
    }
}
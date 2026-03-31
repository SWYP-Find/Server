package com.swyp.picke.domain.reward.service;

import com.google.crypto.tink.apps.rewardedads.RewardedAdsVerifier;
import com.swyp.picke.domain.reward.dto.request.AdMobRewardRequest;
import com.swyp.picke.domain.reward.entity.AdRewardHistory;
import com.swyp.picke.domain.reward.repository.AdRewardHistoryRepository;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.enums.UserRole;
import com.swyp.picke.domain.user.enums.UserStatus;
import com.swyp.picke.domain.user.enums.CreditType;
import com.swyp.picke.domain.user.repository.UserRepository;
import com.swyp.picke.domain.user.service.CreditService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
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

    @Mock
    private CreditService creditService;

    @Test
    @DisplayName("// 1. 정상적인 광고 시청 시 보상 이력이 저장되고 크레딧이 적립된다.")
    void processReward_Success() throws Exception {
        // // 1.1 변경된 구조의 샘플 리퀘스트 생성
        AdMobRewardRequest request = createSampleRequest("unique-id");

        User mockUser = User.builder()
                .userTag("testTag")
                .nickname("시영")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(mockUser, "id", 1L);

        given(adRewardHistoryRepository.existsByTransactionId(request.transaction_id())).willReturn(false);
        // // 1.2 서명 검증은 주석 해제 대비 mock 처리 유지
        // doNothing().when(rewardedAdsVerifier).verify(anyString());
        given(userRepository.findById(1L)).willReturn(Optional.of(mockUser));

        AdRewardHistory savedHistory = AdRewardHistory.builder().build();
        ReflectionTestUtils.setField(savedHistory, "id", 100L);
        given(adRewardHistoryRepository.save(any(AdRewardHistory.class))).willReturn(savedHistory);

        // when
        String result = rewardService.processReward(request);

        // then
        assertThat(result).isEqualTo("OK");
        verify(creditService, times(1)).addCredit(eq(1L), eq(CreditType.AD_REWARD), anyLong());
        verify(adRewardHistoryRepository, times(1)).save(any(AdRewardHistory.class));
    }

    // // 2. DTO 구조 변경에 따른 헬퍼 메서드 수정
    private AdMobRewardRequest createSampleRequest(String transId) {
        return new AdMobRewardRequest(
                "ad-network-123",      // ad_network 추가
                "ad-unit-123",         // ad_unit (기존 ad_unit_id)
                "1",                   // custom_data (유저 ID 1L로 인식됨)
                100,                   // reward_amount
                "POINT",               // reward_item
                123456789L,            // timestamp
                transId,               // transaction_id
                "sig-123",             // signature
                "key-123",             // key_id
                "1"                    // user_id 추가
        );
    }
}
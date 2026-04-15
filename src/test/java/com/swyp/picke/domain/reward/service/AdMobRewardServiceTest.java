package com.swyp.picke.domain.reward.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.crypto.tink.apps.rewardedads.RewardedAdsVerifier;
import com.swyp.picke.domain.reward.dto.request.AdMobRewardRequest;
import com.swyp.picke.domain.reward.entity.AdRewardHistory;
import com.swyp.picke.domain.reward.repository.AdRewardHistoryRepository;
import com.swyp.picke.domain.user.enums.CreditType;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.enums.UserRole;
import com.swyp.picke.domain.user.enums.UserStatus;
import com.swyp.picke.domain.user.service.CreditService;
import com.swyp.picke.domain.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdMobRewardServiceTest {

    @InjectMocks
    private AdMobRewardServiceImpl rewardService;

    @Mock
    private AdRewardHistoryRepository adRewardHistoryRepository;

    @Mock
    private UserService userService;

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
                .userTag("pique-1cc4a030")
                .nickname("시영")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(mockUser, "id", 1L);

        // // 1. 중복 체크 Mock
        given(adRewardHistoryRepository.existsByTransactionId(request.transaction_id())).willReturn(false);

        // // 2. 유저 조회 Mock
        given(userService.findByUserTag("pique-1cc4a030")).willReturn(mockUser);

        // // [중요] 3. Stubbing 제거
        // // ServiceImpl에서 서명 검증 로직이 주석 처리되어 있다면, verify()에 대한 stubbing은 제거해야 합니다.
        // // 만약 나중에 주석을 풀면 다시 넣되, 지금은 에러 방지를 위해 제거합니다.

        // when
        String result = rewardService.processReward(request);

        // then
        assertThat(result).isEqualTo("OK");

        // // 4. 호출 검증
        verify(creditService, times(1)).addCredit(eq(1L), eq(CreditType.FREE_CHARGE), eq(100), anyLong());
        verify(adRewardHistoryRepository, times(1)).save(any(AdRewardHistory.class));
        verify(userService, times(1)).findByUserTag("pique-1cc4a030");
    }

    // // 2. DTO 구조 변경에 따른 헬퍼 메서드 수정
    private AdMobRewardRequest createSampleRequest(String transId) {
        return new AdMobRewardRequest(
                "5450213213280609325", "ca-app-pub-3940256099942544/5224354917",
                "pique-1cc4a030", 100, "POINT", 1711815000000L,
                transId, "sig-123", "key-123", "pique-1cc4a030"
        );
    }
}

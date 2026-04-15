package com.swyp.picke.domain.battle.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.swyp.picke.domain.battle.dto.request.BattleProposalRequest;
import com.swyp.picke.domain.battle.dto.response.BattleProposalResponse;
import com.swyp.picke.domain.battle.enums.BattleCategory;
import com.swyp.picke.domain.battle.repository.BattleProposalRepository;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.enums.CreditType;
import com.swyp.picke.domain.user.service.CreditService;
import com.swyp.picke.domain.user.service.UserService;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BattleProposalServiceTest {

    @InjectMocks
    private BattleProposalService battleProposalService;

    @Mock
    private BattleProposalRepository battleProposalRepository;

    @Mock
    private CreditService creditService;

    @Mock
    private UserService userService;

    @Test
    @DisplayName("1. 배틀 제안 성공 - 크레딧 차감 및 저장 확인")
    void propose_Success() {
        // given
        User user = mock(User.class);
        given(user.getId()).willReturn(1L);
        given(userService.findCurrentUser()).willReturn(user);
        given(creditService.getTotalPoints(1L)).willReturn(100); // 잔액 충분

        BattleProposalRequest request = mock(BattleProposalRequest.class);
        given(request.getCategory()).willReturn(BattleCategory.PHILOSOPHY);
        given(request.getTopic()).willReturn("테스트 주제");

        // when
        BattleProposalResponse response = battleProposalService.propose(request);

        // then
        // 제안 저장 메서드가 호출되었는지 확인
        verify(battleProposalRepository, times(1)).save(any());
        // 크레딧 차감(-30) 로직이 호출되었는지 확인
        verify(creditService, times(1)).addCredit(eq(1L), eq(CreditType.TOPIC_SUGGEST), eq(-30), any());
    }

    @Test
    @DisplayName("2. 배틀 제안 실패 - 크레딧 부족 시 예외 발생")
    void propose_Fail_CreditNotEnough() {
        // given
        User user = mock(User.class);
        given(user.getId()).willReturn(1L);
        given(userService.findCurrentUser()).willReturn(user);
        given(creditService.getTotalPoints(1L)).willReturn(10); // 잔액 부족 (30 미만)

        BattleProposalRequest request = mock(BattleProposalRequest.class);

        // when & then
        // 에러 코드 CREDIT_NOT_ENOUGH가 발생하는지 확인
        CustomException exception = assertThrows(CustomException.class, () -> {
            battleProposalService.propose(request);
        });
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CREDIT_NOT_ENOUGH);
    }
}

package com.swyp.app.domain.user.service;

import com.swyp.app.domain.user.entity.CreditHistory;
import com.swyp.app.domain.user.entity.TierCode;
import com.swyp.app.domain.user.entity.User;
import com.swyp.app.domain.user.enums.CreditType;
import com.swyp.app.domain.user.repository.CreditHistoryRepository;
import com.swyp.app.global.common.exception.CustomException;
import com.swyp.app.global.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditServiceTest {

    @Mock
    private CreditHistoryRepository creditHistoryRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private CreditService creditService;

    @Test
    @DisplayName("현재 로그인 유저에게 기본 크레딧을 적립한다")
    void addCredit_forCurrentUser_savesDefaultAmount() {
        User user = org.mockito.Mockito.mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(userService.findCurrentUser()).thenReturn(user);

        creditService.addCredit(CreditType.BATTLE_VOTE, 10L);

        ArgumentCaptor<CreditHistory> captor = ArgumentCaptor.forClass(CreditHistory.class);
        verify(creditHistoryRepository).saveAndFlush(captor.capture());

        CreditHistory saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getCreditType()).isEqualTo(CreditType.BATTLE_VOTE);
        assertThat(saved.getAmount()).isEqualTo(CreditType.BATTLE_VOTE.getDefaultAmount());
        assertThat(saved.getReferenceId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("referenceId가 없으면 적립을 거부한다")
    void addCredit_withoutReferenceId_throwsException() {
        assertThatThrownBy(() -> creditService.addCredit(1L, CreditType.BATTLE_VOTE, 10, null))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CREDIT_REFERENCE_REQUIRED);

        verify(creditHistoryRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("중복 적립 충돌이면 조용히 무시한다")
    void addCredit_duplicateInsert_ignoresConflict() {
        when(creditHistoryRepository.saveAndFlush(any(CreditHistory.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));
        when(creditHistoryRepository.existsByUserIdAndCreditTypeAndReferenceId(1L, CreditType.BATTLE_VOTE, 10L))
                .thenReturn(true);

        creditService.addCredit(1L, CreditType.BATTLE_VOTE, 10, 10L);

        verify(creditHistoryRepository).existsByUserIdAndCreditTypeAndReferenceId(1L, CreditType.BATTLE_VOTE, 10L);
    }

    @Test
    @DisplayName("중복이 아닌 데이터 무결성 오류는 그대로 던진다")
    void addCredit_nonDuplicateIntegrityFailure_rethrows() {
        when(creditHistoryRepository.saveAndFlush(any(CreditHistory.class)))
                .thenThrow(new DataIntegrityViolationException("broken"));
        when(creditHistoryRepository.existsByUserIdAndCreditTypeAndReferenceId(1L, CreditType.BATTLE_VOTE, 10L))
                .thenReturn(false);

        assertThatThrownBy(() -> creditService.addCredit(1L, CreditType.BATTLE_VOTE, 10, 10L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CREDIT_SAVE_FAILED);
    }

    @Test
    @DisplayName("누적 포인트로 티어를 계산한다")
    void getTier_returnsTierFromTotalPoints() {
        when(creditHistoryRepository.sumAmountByUserId(eq(1L))).thenReturn(2_500);

        TierCode tier = creditService.getTier(1L);

        assertThat(tier).isEqualTo(TierCode.SAGE);
    }
}

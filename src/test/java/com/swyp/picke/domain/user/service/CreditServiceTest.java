package com.swyp.picke.domain.user.service;

import com.swyp.picke.domain.user.entity.CreditHistory;
import com.swyp.picke.domain.user.enums.TierCode;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.enums.CreditType;
import com.swyp.picke.domain.user.enums.UserRole;
import com.swyp.picke.domain.user.enums.UserStatus;
import com.swyp.picke.domain.user.repository.CreditHistoryRepository;
import com.swyp.picke.domain.user.repository.UserRepository;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditServiceTest {

    @Mock
    private CreditHistoryRepository creditHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private CreditService creditService;

    private User newUser(Long id, int initialCredit) {
        User user = User.builder()
                .userTag("tag-" + id)
                .nickname("nick")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        if (initialCredit != 0) {
            user.addCredit(initialCredit);
        }
        return user;
    }

    @Test
    @DisplayName("현재 로그인 유저에게 기본 크레딧을 적립하고 User.credit 캐시에도 반영한다")
    void addCredit_forCurrentUser_savesDefaultAmount() {
        User user = newUser(1L, 0);
        when(userService.findCurrentUser()).thenReturn(user);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.incrementCredit(1L, CreditType.BATTLE_VOTE.getDefaultAmount())).thenReturn(1);

        creditService.addCredit(CreditType.BATTLE_VOTE, 10L);

        ArgumentCaptor<CreditHistory> captor = ArgumentCaptor.forClass(CreditHistory.class);
        verify(creditHistoryRepository).saveAndFlush(captor.capture());

        CreditHistory saved = captor.getValue();
        assertThat(saved.getUser().getId()).isEqualTo(1L);
        assertThat(saved.getCreditType()).isEqualTo(CreditType.BATTLE_VOTE);
        assertThat(saved.getAmount()).isEqualTo(CreditType.BATTLE_VOTE.getDefaultAmount());
        assertThat(saved.getReferenceId()).isEqualTo(10L);
        verify(userRepository).incrementCredit(1L, CreditType.BATTLE_VOTE.getDefaultAmount());
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
    @DisplayName("중복 적립 충돌이면 조용히 무시하고 캐시도 증가시키지 않는다")
    void addCredit_duplicateInsert_ignoresConflict() {
        User user = newUser(1L, 7);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(creditHistoryRepository.saveAndFlush(any(CreditHistory.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));
        when(creditHistoryRepository.existsByUserIdAndCreditTypeAndReferenceId(1L, CreditType.BATTLE_VOTE, 10L))
                .thenReturn(true);

        creditService.addCredit(1L, CreditType.BATTLE_VOTE, 5, 10L);

        verify(creditHistoryRepository).existsByUserIdAndCreditTypeAndReferenceId(1L, CreditType.BATTLE_VOTE, 10L);
        verify(userRepository, never()).incrementCredit(1L, 5);
    }

    @Test
    @DisplayName("차감 시 잔액이 충분하면 credit 캐시를 감소시킨다")
    void addCredit_negativeAmount_decrementsCreditWhenEnough() {
        User user = newUser(1L, 20);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.decrementCreditIfEnough(1L, 10)).thenReturn(1);

        creditService.addCredit(1L, CreditType.BATTLE_ENTRY, -10, 99L);

        verify(userRepository).decrementCreditIfEnough(1L, 10);
        verify(userRepository, never()).incrementCredit(1L, -10);
    }

    @Test
    @DisplayName("차감 시 잔액이 부족하면 CREDIT_NOT_ENOUGH 를 던진다")
    void addCredit_negativeAmount_throwsWhenInsufficient() {
        User user = newUser(1L, 5);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.decrementCreditIfEnough(1L, 10)).thenReturn(0);

        assertThatThrownBy(() -> creditService.addCredit(1L, CreditType.BATTLE_ENTRY, -10, 99L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CREDIT_NOT_ENOUGH);

        verify(userRepository).decrementCreditIfEnough(1L, 10);
        verify(userRepository, never()).incrementCredit(1L, -10);
    }

    @Test
    @DisplayName("중복이 아닌 데이터 무결성 오류는 CREDIT_SAVE_FAILED 로 재기동하고 캐시도 증가시키지 않는다")
    void addCredit_nonDuplicateIntegrityFailure_rethrows() {
        User user = newUser(1L, 3);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(creditHistoryRepository.saveAndFlush(any(CreditHistory.class)))
                .thenThrow(new DataIntegrityViolationException("broken"));
        when(creditHistoryRepository.existsByUserIdAndCreditTypeAndReferenceId(1L, CreditType.BATTLE_VOTE, 10L))
                .thenReturn(false);

        assertThatThrownBy(() -> creditService.addCredit(1L, CreditType.BATTLE_VOTE, 10, 10L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CREDIT_SAVE_FAILED);

        verify(userRepository, never()).incrementCredit(1L, 10);
        verify(userRepository, never()).decrementCreditIfEnough(1L, 10);
    }

    @Test
    @DisplayName("getTotalPoints 는 User.credit 캐시 값을 반환한다 (히스토리 집계 아님)")
    void getTotalPoints_readsUserCreditField() {
        when(userRepository.findCreditById(1L)).thenReturn(2_500);

        int total = creditService.getTotalPoints(1L);

        assertThat(total).isEqualTo(2_500);
        verify(creditHistoryRepository, never()).sumAmountByUserId(any());
    }

    @Test
    @DisplayName("누적 포인트로 티어를 계산한다")
    void getTier_returnsTierFromTotalPoints() {
        when(userRepository.findCreditById(1L)).thenReturn(2_500);

        TierCode tier = creditService.getTier(1L);

        assertThat(tier).isEqualTo(TierCode.SAGE);
    }
}

package com.swyp.picke.domain.user.service;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.user.dto.converter.UserBattleConverter;
import com.swyp.picke.domain.user.dto.response.UserBattleStatusResponse;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.entity.UserBattle;
import com.swyp.picke.domain.user.enums.UserBattleStep;
import com.swyp.picke.domain.user.repository.UserBattleRepository;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserBattleServiceTest {

    @Mock private UserBattleRepository userBattleRepository;
    @Mock private UserBattleConverter userBattleConverter;

    @InjectMocks private UserBattleService userBattleService;

    private User user;
    private Battle battle;

    @BeforeEach
    void setUp() {
        user = mock(User.class);
        battle = mock(Battle.class);
        lenient().when(battle.getId()).thenReturn(1L);
    }

    // --- [조회 테스트] ---

    @Test
    @DisplayName("기록이 있는 유저 조회 시 해당 단계를 반환한다")
    void getUserBattleStatus_Success() {
        // given
        UserBattle userBattle = UserBattle.builder().user(user).battle(battle).step(UserBattleStep.PRE_VOTE).build();
        when(userBattleRepository.findByUserAndBattle(user, battle)).thenReturn(Optional.of(userBattle));
        when(userBattleConverter.toStatusResponse(userBattle)).thenReturn(new UserBattleStatusResponse(1L, UserBattleStep.PRE_VOTE));

        // when
        UserBattleStatusResponse response = userBattleService.getUserBattleStatus(user, battle);

        // then
        assertThat(response.step()).isEqualTo(UserBattleStep.PRE_VOTE);
    }

    @Test
    @DisplayName("기록이 없는 유저 조회 시 INITIAL(NONE) 상태를 반환한다")
    void getUserBattleStatus_ReturnsInitial_WhenEmpty() {
        // given
        when(userBattleRepository.findByUserAndBattle(user, battle)).thenReturn(Optional.empty());
        when(userBattleConverter.toInitialResponse(1L)).thenReturn(new UserBattleStatusResponse(1L, UserBattleStep.NONE));

        // when
        UserBattleStatusResponse response = userBattleService.getUserBattleStatus(user, battle);

        // then
        assertThat(response.step()).isEqualTo(UserBattleStep.NONE);
        verify(userBattleConverter).toInitialResponse(1L);
    }

    // --- [업데이트(Upsert) 테스트] ---

    @Test
    @DisplayName("새로운 배틀 참여 시 UserBattle 레코드를 새로 생성한다")
    void upsertStep_CreatesNewRecord() {
        // given
        when(userBattleRepository.findByUserAndBattle(user, battle)).thenReturn(Optional.empty());

        // when
        userBattleService.upsertStep(user, battle, UserBattleStep.PRE_VOTE);

        // then
        verify(userBattleRepository).save(any(UserBattle.class));
    }

    @Test
    @DisplayName("이미 참여 중인 배틀의 단계를 업데이트한다")
    void upsertStep_UpdatesExistingRecord() {
        // given
        UserBattle existingRecord = spy(UserBattle.builder().user(user).battle(battle).step(UserBattleStep.PRE_VOTE).build());
        when(userBattleRepository.findByUserAndBattle(user, battle)).thenReturn(Optional.of(existingRecord));

        // when
        userBattleService.upsertStep(user, battle, UserBattleStep.COMPLETED);

        // then
        assertThat(existingRecord.getStep()).isEqualTo(UserBattleStep.COMPLETED);
        // 별도의 save 없이 Dirty Checking으로 업데이트되거나 로직상 호출될 수 있음
    }

    // --- [예외 및 경계 케이스] ---

    @Test
    @DisplayName("단계를 이전 단계로 되돌리려 할 때의 방어 로직 확인 (비즈니스 정책에 따라 설정)")
    void upsertStep_ShouldHandleReverseTransition() {
        // 기획상 COMPLETED에서 PRE_VOTE로 돌아가는 것을 막아야 한다면 여기에 검증 로직 추가
        // 현재 로직은 단순 덮어쓰기라면 상태 업데이트 여부만 확인
        UserBattle existingRecord = UserBattle.builder().user(user).battle(battle).step(UserBattleStep.COMPLETED).build();
        when(userBattleRepository.findByUserAndBattle(user, battle)).thenReturn(Optional.of(existingRecord));

        userBattleService.upsertStep(user, battle, UserBattleStep.PRE_VOTE);

        assertThat(existingRecord.getStep()).isEqualTo(UserBattleStep.PRE_VOTE);
    }
}
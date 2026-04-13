package com.swyp.picke.domain.user.service.batch;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.battle.enums.BattleStatus;
import com.swyp.picke.domain.battle.repository.BattleOptionRepository;
import com.swyp.picke.domain.battle.repository.BattleRepository;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.enums.CreditType;
import com.swyp.picke.domain.user.enums.UserRole;
import com.swyp.picke.domain.user.enums.UserStatus;
import com.swyp.picke.domain.user.service.CreditService;
import com.swyp.picke.domain.vote.entity.BattleVote;
import com.swyp.picke.domain.vote.repository.BattleVoteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MajorityWinRewardJobTest {

    @Mock private BattleRepository battleRepository;
    @Mock private BattleOptionRepository battleOptionRepository;
    @Mock private BattleVoteRepository battleVoteRepository;
    @Mock private CreditService creditService;

    @InjectMocks
    private MajorityWinRewardJob job;

    @Test
    @DisplayName("runDate 기준 14~20일 전 targetDate 윈도우로 배틀을 조회한다")
    void run_queriesBattlesInTwoWeeksPriorWindow() {
        LocalDate runDate = LocalDate.of(2026, 4, 13);
        when(battleRepository.findByTargetDateBetweenAndStatusAndDeletedAtIsNull(
                LocalDate.of(2026, 3, 24), LocalDate.of(2026, 3, 30), BattleStatus.PUBLISHED))
                .thenReturn(List.of());

        job.run(runDate);

        verify(battleRepository).findByTargetDateBetweenAndStatusAndDeletedAtIsNull(
                LocalDate.of(2026, 3, 24), LocalDate.of(2026, 3, 30), BattleStatus.PUBLISHED);
    }

    @Test
    @DisplayName("최다 득표 옵션을 사전 투표한 사용자에게만 MAJORITY_WIN 을 지급한다")
    void run_rewardsOnlyWinningOptionVoters() {
        LocalDate runDate = LocalDate.of(2026, 4, 13);
        Battle battle = battle(100L);
        BattleOption winner = option(1L, battle);
        BattleOption loser = option(2L, battle);

        when(battleRepository.findByTargetDateBetweenAndStatusAndDeletedAtIsNull(any(), any(), any()))
                .thenReturn(List.of(battle));
        when(battleOptionRepository.findByBattle(battle)).thenReturn(List.of(winner, loser));
        when(battleVoteRepository.countByBattleAndPreVoteOption(battle, winner)).thenReturn(10L);
        when(battleVoteRepository.countByBattleAndPreVoteOption(battle, loser)).thenReturn(5L);

        User userA = user(11L);
        User userB = user(12L);
        User userC = user(13L);
        BattleVote winVoteA = vote(userA, winner);
        BattleVote winVoteB = vote(userB, winner);
        BattleVote lossVoteC = vote(userC, loser);
        when(battleVoteRepository.findAllByBattle(battle)).thenReturn(List.of(winVoteA, winVoteB, lossVoteC));

        job.run(runDate);

        verify(creditService).addCredit(11L, CreditType.MAJORITY_WIN, 100L);
        verify(creditService).addCredit(12L, CreditType.MAJORITY_WIN, 100L);
        verify(creditService, never()).addCredit(eq(13L), eq(CreditType.MAJORITY_WIN), any());
    }

    private Battle battle(Long id) {
        Battle b = Battle.builder().title("t").build();
        ReflectionTestUtils.setField(b, "id", id);
        return b;
    }

    private BattleOption option(Long id, Battle battle) {
        BattleOption o = BattleOption.builder().battle(battle).title("t").build();
        ReflectionTestUtils.setField(o, "id", id);
        return o;
    }

    private User user(Long id) {
        User u = User.builder().userTag("u" + id).role(UserRole.USER).status(UserStatus.ACTIVE).build();
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    private BattleVote vote(User user, BattleOption preOption) {
        return BattleVote.builder().user(user).battle(preOption.getBattle()).preVoteOption(preOption).build();
    }
}

package com.swyp.picke.domain.user.service.batch;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.battle.enums.BattleOptionLabel;
import com.swyp.picke.domain.battle.enums.BattleStatus;
import com.swyp.picke.domain.battle.repository.BattleRepository;
import com.swyp.picke.domain.perspective.entity.Perspective;
import com.swyp.picke.domain.perspective.enums.PerspectiveStatus;
import com.swyp.picke.domain.perspective.repository.PerspectiveRepository;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.enums.CreditType;
import com.swyp.picke.domain.user.enums.UserRole;
import com.swyp.picke.domain.user.enums.UserStatus;
import com.swyp.picke.domain.user.service.CreditService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BestCommentRewardJobTest {

    @Mock
    private BattleRepository battleRepository;

    @Mock
    private PerspectiveRepository perspectiveRepository;

    @Mock
    private CreditService creditService;

    @InjectMocks
    private BestCommentRewardJob job;

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
    @DisplayName("좋아요가 10개 미만이면 베댓 보상을 지급하지 않는다")
    void run_skipsWhenPerspectiveHasLessThanMinimumLikes() {
        Battle battle = battle(100L);
        Perspective perspective = perspective(200L, battle, user(10L), 9);

        when(battleRepository.findByTargetDateBetweenAndStatusAndDeletedAtIsNull(any(), any(), any()))
                .thenReturn(List.of(battle));
        when(perspectiveRepository.findByBattleIdAndStatusOrderByLikeCountDescCreatedAtDesc(
                battle.getId(), PerspectiveStatus.PUBLISHED, PageRequest.of(0, 3)))
                .thenReturn(List.of(perspective));

        job.run(LocalDate.of(2026, 4, 13));

        verify(creditService, never()).addCredit(any(), any(), any());
    }

    @Test
    @DisplayName("좋아요 상위 3개 Perspective 작성자에게 BEST_COMMENT 를 지급한다")
    void run_rewardsTopThreePerspectiveAuthors() {
        Battle battle = battle(100L);
        User author1 = user(10L);
        User author2 = user(11L);
        User author3 = user(12L);
        Perspective top1 = perspective(200L, battle, author1, 20);
        Perspective top2 = perspective(201L, battle, author2, 15);
        Perspective top3 = perspective(202L, battle, author3, 10);

        when(battleRepository.findByTargetDateBetweenAndStatusAndDeletedAtIsNull(any(), any(), any()))
                .thenReturn(List.of(battle));
        when(perspectiveRepository.findByBattleIdAndStatusOrderByLikeCountDescCreatedAtDesc(
                battle.getId(), PerspectiveStatus.PUBLISHED, PageRequest.of(0, 3)))
                .thenReturn(List.of(top1, top2, top3));

        job.run(LocalDate.of(2026, 4, 13));

        verify(creditService).addCredit(10L, CreditType.BEST_COMMENT, 200L);
        verify(creditService).addCredit(11L, CreditType.BEST_COMMENT, 201L);
        verify(creditService).addCredit(12L, CreditType.BEST_COMMENT, 202L);
        verify(creditService, never()).addCredit(13L, CreditType.BEST_COMMENT, 203L);
    }

    private Battle battle(Long id) {
        Battle battle = Battle.builder()
                .title("battle")
                .status(BattleStatus.PUBLISHED)
                .build();
        ReflectionTestUtils.setField(battle, "id", id);
        return battle;
    }

    private User user(Long id) {
        User user = User.builder()
                .userTag("user-" + id)
                .nickname("nick")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Perspective perspective(Long id, Battle battle, User user, int likeCount) {
        BattleOption option = BattleOption.builder()
                .battle(battle)
                .label(BattleOptionLabel.A)
                .title("A")
                .stance("stance")
                .build();

        Perspective perspective = Perspective.builder()
                .battle(battle)
                .user(user)
                .option(option)
                .content("content")
                .build();
        perspective.publish();
        while (perspective.getLikeCount() < likeCount) {
            perspective.incrementLikeCount();
        }
        ReflectionTestUtils.setField(perspective, "id", id);
        return perspective;
    }
}

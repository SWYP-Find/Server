package com.swyp.picke.domain.vote.service;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.battle.enums.BattleOptionLabel;
import com.swyp.picke.domain.battle.enums.BattleStatus;
import com.swyp.picke.domain.battle.repository.BattleOptionRepository;
import com.swyp.picke.domain.battle.service.BattleService;
import com.swyp.picke.domain.user.dto.response.UserBattleStatusResponse;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.enums.CreditType;
import com.swyp.picke.domain.user.enums.UserBattleStep;
import com.swyp.picke.domain.user.enums.UserRole;
import com.swyp.picke.domain.user.enums.UserStatus;
import com.swyp.picke.domain.user.repository.UserRepository;
import com.swyp.picke.domain.user.service.CreditService;
import com.swyp.picke.domain.user.service.UserBattleService;
import com.swyp.picke.domain.vote.dto.request.VoteRequest;
import com.swyp.picke.domain.vote.dto.response.VoteResultResponse;
import com.swyp.picke.domain.vote.entity.BattleVote;
import com.swyp.picke.domain.vote.repository.BattleVoteRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BattleVoteServiceImplTest {

    @Mock
    private BattleVoteRepository battleVoteRepository;

    @Mock
    private BattleService battleService;

    @Mock
    private BattleOptionRepository battleOptionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserBattleService userBattleService;

    @Mock
    private CreditService creditService;

    @InjectMocks
    private BattleVoteServiceImpl battleVoteService;

    @Test
    @DisplayName("사후 투표 완료 시 참여 보상 크레딧을 지급한다")
    void postVote_rewardsBattleParticipationCredit() {
        Battle battle = battle(100L);
        User user = user(10L);
        BattleOption preOption = option(201L, battle, BattleOptionLabel.A);
        BattleOption postOption = option(202L, battle, BattleOptionLabel.B);
        BattleVote vote = BattleVote.builder()
                .user(user)
                .battle(battle)
                .preVoteOption(preOption)
                .build();
        ReflectionTestUtils.setField(vote, "id", 300L);

        when(battleService.findById(100L)).thenReturn(battle);
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(battleOptionRepository.findById(202L)).thenReturn(Optional.of(postOption));
        when(battleVoteRepository.findByBattleAndUser(battle, user)).thenReturn(Optional.of(vote));
        when(userBattleService.getUserBattleStatus(user, battle))
                .thenReturn(new UserBattleStatusResponse(100L, UserBattleStep.POST_VOTE));

        VoteResultResponse response = battleVoteService.postVote(100L, 10L, new VoteRequest(202L));

        assertThat(vote.getPostVoteOption()).isEqualTo(postOption);
        assertThat(response.voteId()).isEqualTo(300L);
        assertThat(response.status()).isEqualTo(UserBattleStep.COMPLETED);
        verify(userBattleService).upsertStep(user, battle, UserBattleStep.COMPLETED);
        verify(creditService).addCredit(10L, CreditType.BATTLE_VOTE, 300L);
    }

    private Battle battle(Long id) {
        Battle battle = Battle.builder()
                .title("battle")
                .summary("summary")
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

    private BattleOption option(Long id, Battle battle, BattleOptionLabel label) {
        BattleOption option = BattleOption.builder()
                .battle(battle)
                .label(label)
                .title(label.name())
                .stance("stance")
                .build();
        ReflectionTestUtils.setField(option, "id", id);
        return option;
    }
}

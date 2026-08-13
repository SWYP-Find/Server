package com.swyp.picke.domain.vote.service;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.battle.enums.BattleOptionLabel;
import com.swyp.picke.domain.battle.enums.BattleStatus;
import com.swyp.picke.domain.battle.repository.BattleOptionRepository;
import com.swyp.picke.domain.battle.repository.BattleOptionTagRepository;
import com.swyp.picke.domain.battle.entity.BattleOptionTag;
import com.swyp.picke.domain.battle.service.BattleService;
import com.swyp.picke.domain.tag.entity.Tag;
import com.swyp.picke.domain.tag.enums.TagType;
import com.swyp.picke.domain.user.dto.response.UserBattleStatusResponse;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.entity.UserTendencyScore;
import com.swyp.picke.domain.user.enums.CreditType;
import com.swyp.picke.domain.user.enums.UserBattleStep;
import com.swyp.picke.domain.user.enums.UserRole;
import com.swyp.picke.domain.user.enums.UserStatus;
import com.swyp.picke.domain.user.repository.UserRepository;
import com.swyp.picke.domain.user.repository.UserTendencyScoreRepository;
import com.swyp.picke.domain.user.service.CreditService;
import com.swyp.picke.domain.user.service.UserBattleService;
import com.swyp.picke.domain.vote.dto.request.VoteRequest;
import com.swyp.picke.domain.vote.dto.response.VoteResultResponse;
import com.swyp.picke.domain.vote.entity.BattleVote;
import com.swyp.picke.domain.vote.repository.BattleVoteRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
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
    private BattleOptionTagRepository battleOptionTagRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserTendencyScoreRepository userTendencyScoreRepository;

    @Mock
    private UserBattleService userBattleService;

    @Mock
    private CreditService creditService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private BattleVoteServiceImpl battleVoteService;

    @Test
    @DisplayName("오늘 배틀이 아니면 최초 사전 투표 시 BATTLE_ENTRY 크레딧을 차감한다")
    void preVote_chargesBattleEntryCreditForPastBattle() {
        Battle battle = battle(100L, LocalDate.now().minusDays(1));
        User user = user(10L);
        BattleOption option = option(201L, battle, BattleOptionLabel.A);

        when(battleService.findById(100L)).thenReturn(battle);
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(battleOptionRepository.findById(201L)).thenReturn(Optional.of(option));
        when(battleVoteRepository.findByBattleAndUser(battle, user)).thenReturn(Optional.empty());
        when(userBattleService.getUserBattleStatus(user, battle))
                .thenReturn(new UserBattleStatusResponse(100L, UserBattleStep.NONE));

        VoteResultResponse response = battleVoteService.preVote(100L, 10L, new VoteRequest(201L));

        assertThat(response.status()).isEqualTo(UserBattleStep.PRE_VOTE);
        verify(creditService).addCredit(10L, CreditType.BATTLE_ENTRY, 100L);
        verify(userBattleService).upsertStep(user, battle, UserBattleStep.PRE_VOTE);
    }

    @Test
    @DisplayName("오늘 배틀이면 최초 사전 투표 시 크레딧을 차감하지 않는다")
    void preVote_doesNotChargeBattleEntryCreditForTodayBattle() {
        Battle battle = battle(100L, LocalDate.now());
        User user = user(10L);
        BattleOption option = option(201L, battle, BattleOptionLabel.A);

        when(battleService.findById(100L)).thenReturn(battle);
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(battleOptionRepository.findById(201L)).thenReturn(Optional.of(option));
        when(battleVoteRepository.findByBattleAndUser(battle, user)).thenReturn(Optional.empty());
        when(userBattleService.getUserBattleStatus(user, battle))
                .thenReturn(new UserBattleStatusResponse(100L, UserBattleStep.NONE));

        VoteResultResponse response = battleVoteService.preVote(100L, 10L, new VoteRequest(201L));

        assertThat(response.status()).isEqualTo(UserBattleStep.PRE_VOTE);
        verify(creditService, never()).addCredit(10L, CreditType.BATTLE_ENTRY, 100L);
    }

    @Test
    @DisplayName("오늘의 배틀이라도 오늘 이미 무료 진입을 사용했다면 크레딧을 차감한다")
    void preVote_chargesBattleEntryCreditWhenFreeEntryAlreadyUsedToday() {
        Battle battle = battle(100L, LocalDate.now());
        User user = user(10L);
        BattleOption option = option(201L, battle, BattleOptionLabel.A);

        when(battleService.findById(100L)).thenReturn(battle);
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(battleOptionRepository.findById(201L)).thenReturn(Optional.of(option));
        when(battleVoteRepository.findByBattleAndUser(battle, user)).thenReturn(Optional.empty());
        when(battleVoteRepository.existsByUserIdAndBattle_TargetDate(10L, LocalDate.now())).thenReturn(true);
        when(userBattleService.getUserBattleStatus(user, battle))
                .thenReturn(new UserBattleStatusResponse(100L, UserBattleStep.NONE));

        VoteResultResponse response = battleVoteService.preVote(100L, 10L, new VoteRequest(201L));

        assertThat(response.status()).isEqualTo(UserBattleStep.PRE_VOTE);
        verify(creditService).addCredit(10L, CreditType.BATTLE_ENTRY, 100L);
    }

    @Test
    @DisplayName("이미 사전 투표한 배틀이면 옵션 변경 시 추가 차감하지 않는다")
    void preVote_doesNotChargeAgainWhenVoteAlreadyExists() {
        Battle battle = battle(100L, LocalDate.now().minusDays(1));
        User user = user(10L);
        BattleOption oldOption = option(200L, battle, BattleOptionLabel.B);
        BattleOption newOption = option(201L, battle, BattleOptionLabel.A);
        BattleVote vote = BattleVote.builder()
                .user(user)
                .battle(battle)
                .preVoteOption(oldOption)
                .build();

        when(battleService.findById(100L)).thenReturn(battle);
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(battleOptionRepository.findById(201L)).thenReturn(Optional.of(newOption));
        when(battleVoteRepository.findByBattleAndUser(battle, user)).thenReturn(Optional.of(vote));
        when(userBattleService.getUserBattleStatus(user, battle))
                .thenReturn(new UserBattleStatusResponse(100L, UserBattleStep.PRE_VOTE));

        VoteResultResponse response = battleVoteService.preVote(100L, 10L, new VoteRequest(201L));

        assertThat(response.status()).isEqualTo(UserBattleStep.PRE_VOTE);
        assertThat(vote.getPreVoteOption()).isEqualTo(newOption);
        verify(creditService, never()).addCredit(10L, CreditType.BATTLE_ENTRY, 100L);
    }

    @Test
    @DisplayName("사후 투표 완료 시 스텝을 완료 상태로 전환한다")
    void postVote_completesUserBattleStep() {
        Battle battle = battle(100L, null);
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
    }

    @Test
    @DisplayName("대표 6개에 없는 반대 가치관도 실제 사후투표 입력 시 -로 계산한다")
    void postVote_appliesNegativeValueTag() {
        Battle battle = battle(100L, null);
        User user = user(10L);
        BattleOption option = option(202L, battle, BattleOptionLabel.B);
        BattleVote vote = BattleVote.builder()
                .user(user)
                .battle(battle)
                .preVoteOption(option)
                .build();
        UserTendencyScore score = UserTendencyScore.builder().user(user).build();
        Tag structure = Tag.builder().name("구조").type(TagType.VALUE).build();
        BattleOptionTag optionTag = BattleOptionTag.builder()
                .battleOption(option)
                .tag(structure)
                .build();

        when(battleService.findById(100L)).thenReturn(battle);
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(battleOptionRepository.findById(202L)).thenReturn(Optional.of(option));
        when(battleVoteRepository.findByBattleAndUser(battle, user)).thenReturn(Optional.of(vote));
        when(userBattleService.getUserBattleStatus(user, battle))
                .thenReturn(new UserBattleStatusResponse(100L, UserBattleStep.POST_VOTE));
        when(userTendencyScoreRepository.findByUserId(10L)).thenReturn(Optional.of(score));
        when(battleOptionTagRepository.findByBattleOption(option)).thenReturn(List.of(optionTag));

        battleVoteService.postVote(100L, 10L, new VoteRequest(202L));

        assertThat(score.getInner()).isEqualTo(-1);
    }

    @Test
    @DisplayName("사후투표를 변경하면 기존 태그 점수를 제거하고 새 태그를 반영한다")
    void postVote_replacesPreviousValueTagScore() {
        Battle battle = battle(100L, null);
        User user = user(10L);
        BattleOption previousOption = option(201L, battle, BattleOptionLabel.A);
        BattleOption newOption = option(202L, battle, BattleOptionLabel.B);
        BattleVote vote = BattleVote.builder()
                .user(user)
                .battle(battle)
                .preVoteOption(previousOption)
                .postVoteOption(previousOption)
                .build();
        UserTendencyScore score = UserTendencyScore.builder().user(user).inner(1).build();
        BattleOptionTag innerTag = BattleOptionTag.builder()
                .battleOption(previousOption)
                .tag(Tag.builder().name("내면").type(TagType.VALUE).build())
                .build();
        BattleOptionTag structureTag = BattleOptionTag.builder()
                .battleOption(newOption)
                .tag(Tag.builder().name("구조").type(TagType.VALUE).build())
                .build();

        when(battleService.findById(100L)).thenReturn(battle);
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(battleOptionRepository.findById(202L)).thenReturn(Optional.of(newOption));
        when(battleVoteRepository.findByBattleAndUser(battle, user)).thenReturn(Optional.of(vote));
        when(userBattleService.getUserBattleStatus(user, battle))
                .thenReturn(new UserBattleStatusResponse(100L, UserBattleStep.COMPLETED));
        when(userTendencyScoreRepository.findByUserId(10L)).thenReturn(Optional.of(score));
        when(battleOptionTagRepository.findByBattleOption(previousOption)).thenReturn(List.of(innerTag));
        when(battleOptionTagRepository.findByBattleOption(newOption)).thenReturn(List.of(structureTag));

        battleVoteService.postVote(100L, 10L, new VoteRequest(202L));

        assertThat(score.getInner()).isEqualTo(-1);
    }

    private Battle battle(Long id, LocalDate targetDate) {
        Battle battle = Battle.builder()
                .title("battle")
                .summary("summary")
                .targetDate(targetDate)
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
                .title(label.name())
                .stance("stance")
                .displayOrder(label == BattleOptionLabel.A ? 1 : 2)
                .build();
        ReflectionTestUtils.setField(option, "id", id);
        return option;
    }
}

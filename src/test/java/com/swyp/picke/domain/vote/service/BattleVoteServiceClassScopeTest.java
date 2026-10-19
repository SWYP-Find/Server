package com.swyp.picke.domain.vote.service;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.battle.enums.BattleStatus;
import com.swyp.picke.domain.battle.repository.BattleOptionRepository;
import com.swyp.picke.domain.battle.repository.BattleOptionTagRepository;
import com.swyp.picke.domain.battle.service.BattleService;
import com.swyp.picke.domain.classroom.entity.Classroom;
import com.swyp.picke.domain.user.dto.response.UserBattleStatusResponse;
import com.swyp.picke.domain.user.entity.User;
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
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 클래스 안의 투표(classroom != null)가 PRD §7 규칙을 지키고 일반 배틀 부수효과(크레딧·참여자 수·성향·SSE)를 내지 않는지 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class BattleVoteServiceClassScopeTest {

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
    @Mock
    private Classroom classroom;

    @InjectMocks
    private BattleVoteServiceImpl service;

    private Battle battle;
    private User user;
    private BattleOption optionA;
    private BattleOption optionB;

    @BeforeEach
    void setUp() {
        battle = battle(100L);
        user = user(10L);
        optionA = option(201L, battle, 1);
        optionB = option(202L, battle, 2);
        when(battleService.findById(100L)).thenReturn(battle);
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
    }

    @Test
    @DisplayName("클래스 첫 사전투표: 지난 배틀이라도 크레딧을 차감하지 않고 일반 참여자 수도 올리지 않으며 class_id 가 붙은 표를 저장한다")
    void preVote_inClass_noCreditNoParticipant() {
        when(battleOptionRepository.findById(201L)).thenReturn(Optional.of(optionA));
        when(userBattleService.getUserBattleStatus(user, battle, classroom))
                .thenReturn(new UserBattleStatusResponse(100L, UserBattleStep.NONE));
        when(battleVoteRepository.findByBattleAndUserAndClassroom(battle, user, classroom)).thenReturn(Optional.empty());

        VoteResultResponse response = service.preVote(100L, 10L, classroom, new VoteRequest(201L));

        assertThat(response.status()).isEqualTo(UserBattleStep.PRE_VOTE);
        verify(creditService, never()).addCredit(anyLong(), any(), anyLong());
        verify(battleVoteRepository, never()).existsByUserIdAndClassroomIsNullAndBattle_TargetDate(anyLong(), any());
        assertThat(battle.getTotalParticipantsCount()).isZero();
        ArgumentCaptor<BattleVote> captor = ArgumentCaptor.forClass(BattleVote.class);
        verify(battleVoteRepository).save(captor.capture());
        assertThat(captor.getValue().getClassroom()).isSameAs(classroom);
        verify(userBattleService).upsertStep(user, battle, classroom, UserBattleStep.PRE_VOTE);
    }

    @Test
    @DisplayName("클래스 사전투표는 PRE_VOTE 단계까지만 변경 가능, TTS 를 들은 뒤(POST_VOTE)에는 CLASS_PRE_VOTE_LOCKED")
    void preVote_inClass_lockedAfterContentStarted() {
        BattleVote existing = BattleVote.createPreVote(user, battle, classroom, optionA);
        when(battleOptionRepository.findById(202L)).thenReturn(Optional.of(optionB));
        when(battleVoteRepository.findByBattleAndUserAndClassroom(battle, user, classroom)).thenReturn(Optional.of(existing));

        when(userBattleService.getUserBattleStatus(user, battle, classroom))
                .thenReturn(new UserBattleStatusResponse(100L, UserBattleStep.PRE_VOTE));
        service.preVote(100L, 10L, classroom, new VoteRequest(202L));
        assertThat(existing.getPreVoteOption()).isSameAs(optionB);

        when(userBattleService.getUserBattleStatus(user, battle, classroom))
                .thenReturn(new UserBattleStatusResponse(100L, UserBattleStep.POST_VOTE));
        when(battleOptionRepository.findById(201L)).thenReturn(Optional.of(optionA));
        assertThatThrownBy(() -> service.preVote(100L, 10L, classroom, new VoteRequest(201L)))
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.CLASS_PRE_VOTE_LOCKED);
        assertThat(existing.getPreVoteOption()).isSameAs(optionB);
    }

    @Test
    @DisplayName("클래스 사후투표: 완료 처리하되 성향 점수·SSE 는 건드리지 않고, 한 번 제출하면 VOTE_ALREADY_SUBMITTED")
    void postVote_inClass_lockedAfterSubmit() {
        BattleVote vote = BattleVote.createPreVote(user, battle, classroom, optionA);
        when(battleOptionRepository.findById(202L)).thenReturn(Optional.of(optionB));
        when(battleVoteRepository.findByBattleAndUserAndClassroom(battle, user, classroom)).thenReturn(Optional.of(vote));
        when(userBattleService.getUserBattleStatus(user, battle, classroom))
                .thenReturn(new UserBattleStatusResponse(100L, UserBattleStep.POST_VOTE));

        VoteResultResponse response = service.postVote(100L, 10L, classroom, new VoteRequest(202L));

        assertThat(response.status()).isEqualTo(UserBattleStep.COMPLETED);
        assertThat(vote.getPostVoteOption()).isSameAs(optionB);
        verify(userBattleService).upsertStep(user, battle, classroom, UserBattleStep.COMPLETED);
        verify(userTendencyScoreRepository, never()).findByUserId(anyLong());
        verify(eventPublisher, never()).publishEvent(any());

        when(userBattleService.getUserBattleStatus(user, battle, classroom))
                .thenReturn(new UserBattleStatusResponse(100L, UserBattleStep.COMPLETED));
        when(battleOptionRepository.findById(201L)).thenReturn(Optional.of(optionA));
        assertThatThrownBy(() -> service.postVote(100L, 10L, classroom, new VoteRequest(201L)))
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.VOTE_ALREADY_SUBMITTED);
        assertThat(vote.getPostVoteOption()).isSameAs(optionB);
    }

    @Test
    @DisplayName("클래스 TTS 다시 듣기: 이미 COMPLETED 면 단계를 POST_VOTE 로 되돌리지 않는다")
    void completeTts_inClass_doesNotDowngradeCompleted() {
        BattleVote vote = BattleVote.createPreVote(user, battle, classroom, optionA);
        when(battleVoteRepository.findByBattleAndUserAndClassroom(battle, user, classroom)).thenReturn(Optional.of(vote));
        when(userBattleService.getUserBattleStatus(user, battle, classroom))
                .thenReturn(new UserBattleStatusResponse(100L, UserBattleStep.COMPLETED));

        service.completeTts(100L, 10L, classroom);

        verify(userBattleService, never()).upsertStep(eq(user), eq(battle), eq(classroom), any());
    }

    @Test
    @DisplayName("다른 배틀의 선택지 ID 로 투표하면 BATTLE_OPTION_NOT_FOUND")
    void preVote_optionOfOtherBattle_rejected() {
        BattleOption foreign = option(999L, battle(200L), 1);
        when(battleOptionRepository.findById(999L)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> service.preVote(100L, 10L, classroom, new VoteRequest(999L)))
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.BATTLE_OPTION_NOT_FOUND);
    }

    // ===== 헬퍼 =====

    private static Battle battle(Long id) {
        Battle battle = Battle.builder()
                .title("battle").summary("summary")
                .targetDate(LocalDate.now().minusDays(3))
                .status(BattleStatus.PUBLISHED)
                .build();
        ReflectionTestUtils.setField(battle, "id", id);
        return battle;
    }

    private static User user(Long id) {
        User user = User.builder().userTag("user-" + id).nickname("nick")
                .role(UserRole.USER).status(UserStatus.ACTIVE).build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static BattleOption option(Long id, Battle battle, int order) {
        BattleOption option = BattleOption.builder()
                .battle(battle).title("opt" + order).stance("stance").displayOrder(order)
                .build();
        ReflectionTestUtils.setField(option, "id", id);
        return option;
    }
}

package com.swyp.picke.domain.classroom.service;

import com.swyp.picke.domain.battle.dto.response.BattleUserDetailResponse;
import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.battle.enums.BattleCreatorType;
import com.swyp.picke.domain.battle.enums.BattleStatus;
import com.swyp.picke.domain.battle.service.BattleService;
import com.swyp.picke.domain.classroom.dto.response.ClassBattleDetailResponse;
import com.swyp.picke.domain.classroom.entity.ClassMember;
import com.swyp.picke.domain.classroom.entity.Classroom;
import com.swyp.picke.domain.classroom.enums.ClassBattleType;
import com.swyp.picke.domain.classroom.enums.ClassMemberRole;
import com.swyp.picke.domain.classroom.service.ClassAccessService.ClassAccess;
import com.swyp.picke.domain.perspective.enums.PerspectiveStatus;
import com.swyp.picke.domain.perspective.repository.PerspectiveRepository;
import com.swyp.picke.domain.perspective.service.PerspectiveService;
import com.swyp.picke.domain.ticket.entity.ClassTicket;
import com.swyp.picke.domain.ticket.entity.FundingOrder;
import com.swyp.picke.domain.ticket.enums.FundingPlatform;
import com.swyp.picke.domain.ticket.enums.PaymentStatus;
import com.swyp.picke.domain.ticket.enums.RewardTier;
import com.swyp.picke.domain.user.dto.response.UserBattleStatusResponse;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.enums.UserBattleStep;
import com.swyp.picke.domain.user.enums.UserRole;
import com.swyp.picke.domain.user.enums.UserStatus;
import com.swyp.picke.domain.user.enums.VoteSide;
import com.swyp.picke.domain.user.repository.UserRepository;
import com.swyp.picke.domain.user.service.UserBattleService;
import com.swyp.picke.domain.vote.dto.request.VoteRequest;
import com.swyp.picke.domain.vote.entity.BattleVote;
import com.swyp.picke.domain.vote.repository.BattleVoteRepository;
import com.swyp.picke.domain.vote.service.BattleVoteService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassBattleServiceImplTest {

    private static final Long OWNER_ID = 1L;
    private static final Long STUDENT_ID = 2L;
    private static final Long CLASS_ID = 100L;
    private static final Long BATTLE_ID = 10L;

    @Mock
    private ClassAccessService classAccessService;
    @Mock
    private BattleService battleService;
    @Mock
    private BattleVoteService battleVoteService;
    @Mock
    private UserBattleService userBattleService;
    @Mock
    private PerspectiveService perspectiveService;
    @Mock
    private BattleVoteRepository battleVoteRepository;
    @Mock
    private PerspectiveRepository perspectiveRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ClassBattleServiceImpl service;

    private User owner;
    private User student;
    private Battle battle;
    private Classroom classroom;
    private BattleUserDetailResponse detail;

    @BeforeEach
    void setUp() {
        owner = user(OWNER_ID, "선생님");
        student = user(STUDENT_ID, "학생");
        battle = battle();
        classroom = classroom(owner, battle, true);
        detail = mock(BattleUserDetailResponse.class);
    }

    @Test
    @DisplayName("상세: 사후투표까지 끝났어도 의견 필수 클래스에서 내 PUBLISHED 의견이 없으면 participationCompleted=false")
    void getDetail_commentRequired_withoutPerspective_notCompleted() {
        ClassMember member = member(student, ClassMemberRole.MEMBER);
        when(classAccessService.requireReadable(CLASS_ID, STUDENT_ID))
                .thenReturn(new ClassAccess(classroom, ClassMemberRole.MEMBER, member));
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
        when(userBattleService.getUserBattleStatus(student, battle, classroom))
                .thenReturn(new UserBattleStatusResponse(BATTLE_ID, UserBattleStep.COMPLETED));
        BattleOption optionB = option(202L, 2);
        BattleVote vote = BattleVote.createPreVote(student, battle, classroom, optionB);
        vote.doPostVote(optionB);
        when(battleVoteRepository.findByBattleAndUserAndClassroom(battle, student, classroom)).thenReturn(Optional.of(vote));
        when(battleVoteRepository.countByBattleAndClassroom(battle, classroom)).thenReturn(3L);
        when(perspectiveRepository.existsByBattleIdAndUserIdAndClassroomIdAndStatus(BATTLE_ID, STUDENT_ID, CLASS_ID, PerspectiveStatus.PUBLISHED))
                .thenReturn(false);
        when(battleService.buildUserDetail(battle, 3L, VoteSide.CON, UserBattleStep.COMPLETED)).thenReturn(detail);

        ClassBattleDetailResponse response = service.getDetail(STUDENT_ID, CLASS_ID);

        assertThat(response.classId()).isEqualTo(CLASS_ID);
        assertThat(response.myRole()).isEqualTo(ClassMemberRole.MEMBER);
        assertThat(response.commentRequired()).isTrue();
        assertThat(response.canParticipate()).isTrue();
        assertThat(response.participationCompleted()).isFalse();
        assertThat(response.closed()).isFalse();
        assertThat(response.battle()).isSameAs(detail);
    }

    @Test
    @DisplayName("상세: 의견이 필수가 아니면 사후투표 완료만으로 participationCompleted=true 이고 의견 존재 여부는 조회하지 않는다")
    void getDetail_commentNotRequired_completedByPostVote() {
        classroom = classroom(owner, battle, false);
        ClassMember member = member(student, ClassMemberRole.MEMBER);
        when(classAccessService.requireReadable(CLASS_ID, STUDENT_ID))
                .thenReturn(new ClassAccess(classroom, ClassMemberRole.MEMBER, member));
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
        when(userBattleService.getUserBattleStatus(student, battle, classroom))
                .thenReturn(new UserBattleStatusResponse(BATTLE_ID, UserBattleStep.COMPLETED));
        when(battleVoteRepository.findByBattleAndUserAndClassroom(battle, student, classroom)).thenReturn(Optional.empty());
        when(battleVoteRepository.countByBattleAndClassroom(battle, classroom)).thenReturn(1L);
        when(battleService.buildUserDetail(battle, 1L, null, UserBattleStep.COMPLETED)).thenReturn(detail);

        ClassBattleDetailResponse response = service.getDetail(STUDENT_ID, CLASS_ID);

        assertThat(response.participationCompleted()).isTrue();
        verify(perspectiveRepository, org.mockito.Mockito.never())
                .existsByBattleIdAndUserIdAndClassroomIdAndStatus(any(), any(), any(), any());
    }

    @Test
    @DisplayName("상세: 참여하지 않는 운영자는 읽을 수 있지만 canParticipate=false, 마감되면 멤버도 false")
    void getDetail_canParticipate() {
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(classAccessService.requireReadable(CLASS_ID, OWNER_ID))
                .thenReturn(new ClassAccess(classroom, ClassMemberRole.OWNER, null));
        when(userBattleService.getUserBattleStatus(owner, battle, classroom))
                .thenReturn(new UserBattleStatusResponse(BATTLE_ID, UserBattleStep.NONE));
        when(battleVoteRepository.findByBattleAndUserAndClassroom(battle, owner, classroom)).thenReturn(Optional.empty());
        when(battleVoteRepository.countByBattleAndClassroom(battle, classroom)).thenReturn(0L);
        when(battleService.buildUserDetail(eq(battle), eq(0L), any(), eq(UserBattleStep.NONE))).thenReturn(detail);

        ClassBattleDetailResponse ownerView = service.getDetail(OWNER_ID, CLASS_ID);
        assertThat(ownerView.myRole()).isEqualTo(ClassMemberRole.OWNER);
        assertThat(ownerView.canParticipate()).isFalse();
        assertThat(ownerView.participationCompleted()).isFalse();

        // 마감일 경과
        ReflectionTestUtils.setField(classroom, "deadline", LocalDateTime.now().minusMinutes(1));
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
        when(classAccessService.requireReadable(CLASS_ID, STUDENT_ID))
                .thenReturn(new ClassAccess(classroom, ClassMemberRole.MEMBER, member(student, ClassMemberRole.MEMBER)));
        when(userBattleService.getUserBattleStatus(student, battle, classroom))
                .thenReturn(new UserBattleStatusResponse(BATTLE_ID, UserBattleStep.NONE));
        when(battleVoteRepository.findByBattleAndUserAndClassroom(battle, student, classroom)).thenReturn(Optional.empty());

        ClassBattleDetailResponse memberView = service.getDetail(STUDENT_ID, CLASS_ID);
        assertThat(memberView.closed()).isTrue();
        assertThat(memberView.canParticipate()).isFalse();
    }

    @Test
    @DisplayName("쓰기(사전투표·의견 등록)는 requireWritable 을 거친 뒤 클래스에 붙은 배틀 ID 와 classroom 으로 위임한다")
    void writes_delegateWithClassroom() {
        ClassMember member = member(student, ClassMemberRole.MEMBER);
        when(classAccessService.requireWritable(eq(CLASS_ID), eq(STUDENT_ID), any(LocalDateTime.class)))
                .thenReturn(new ClassAccess(classroom, ClassMemberRole.MEMBER, member));

        VoteRequest request = new VoteRequest(201L);
        service.preVote(STUDENT_ID, CLASS_ID, request);
        service.completeTts(STUDENT_ID, CLASS_ID);
        service.postVote(STUDENT_ID, CLASS_ID, request);

        verify(battleVoteService).preVote(BATTLE_ID, STUDENT_ID, classroom, request);
        verify(battleVoteService).completeTts(BATTLE_ID, STUDENT_ID, classroom);
        verify(battleVoteService).postVote(BATTLE_ID, STUDENT_ID, classroom, request);
    }

    @Test
    @DisplayName("읽기(통계·의견 목록)는 requireReadable 만 거치고 classroom 으로 위임한다")
    void reads_delegateWithClassroom() {
        when(classAccessService.requireReadable(CLASS_ID, OWNER_ID))
                .thenReturn(new ClassAccess(classroom, ClassMemberRole.OWNER, null));

        service.getVoteStats(OWNER_ID, CLASS_ID);
        service.getPerspectives(OWNER_ID, CLASS_ID, null, 20, null, "popular");
        service.getMyPerspectives(OWNER_ID, CLASS_ID);

        verify(battleVoteService).getVoteStats(BATTLE_ID, classroom);
        verify(perspectiveService).getPerspectives(BATTLE_ID, OWNER_ID, classroom, null, 20, null, "popular");
        verify(perspectiveService).getMyPerspective(BATTLE_ID, OWNER_ID, classroom);
        verify(classAccessService, org.mockito.Mockito.never()).requireWritable(any(Long.class), any(), any());
    }

    // ===== 헬퍼 =====

    private ClassMember member(User user, ClassMemberRole role) {
        return ClassMember.builder()
                .classroom(classroom).user(user).displayName(user.getNickname()).role(role)
                .joinedAt(LocalDateTime.now().minusHours(1))
                .build();
    }

    private static Classroom classroom(User owner, Battle battle, boolean commentRequired) {
        FundingOrder order = FundingOrder.builder()
                .platform(FundingPlatform.TUMBLBUG).campaignId("camp").orderId("order-1")
                .rewardTier(RewardTier.SEAT_5).quantity(1).paymentStatus(PaymentStatus.PAID_CONFIRMED)
                .paidAt(LocalDateTime.now().minusDays(2)).build();
        ClassTicket ticket = ClassTicket.issue(order, "hash-1");
        ticket.register(owner, LocalDateTime.now().minusDays(1));
        Classroom c = Classroom.builder()
                .name("클래스").inviteCode("ABC234").owner(owner).ticket(ticket).battle(battle)
                .battleType(ClassBattleType.TTS).capacity(5).deadline(null)
                .commentRequired(commentRequired).ownerParticipates(false)
                .build();
        return withId(c, CLASS_ID);
    }

    private static Battle battle() {
        Battle battle = Battle.builder()
                .title("배틀").summary("요약").description("설명").thumbnailUrl("https://img/1.png")
                .targetDate(LocalDate.now()).status(BattleStatus.PUBLISHED).creatorType(BattleCreatorType.ADMIN)
                .build();
        return withId(battle, BATTLE_ID);
    }

    private BattleOption option(Long id, int order) {
        BattleOption option = BattleOption.builder()
                .battle(battle).title("opt" + order).stance("stance").displayOrder(order)
                .build();
        return withId(option, id);
    }

    private static User user(Long id, String nickname) {
        User user = User.builder().userTag("u-" + id).nickname(nickname)
                .role(UserRole.USER).status(UserStatus.ACTIVE).build();
        return withId(user, id);
    }

    private static <T> T withId(T entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
}

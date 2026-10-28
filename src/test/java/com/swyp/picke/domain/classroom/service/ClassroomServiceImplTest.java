package com.swyp.picke.domain.classroom.service;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.enums.BattleCreatorType;
import com.swyp.picke.domain.battle.enums.BattleStatus;
import com.swyp.picke.domain.battle.repository.BattleRepository;
import com.swyp.picke.domain.classroom.dto.request.ClassCreateRequest;
import com.swyp.picke.domain.classroom.dto.request.ClassUpdateRequest;
import com.swyp.picke.domain.classroom.dto.response.ClassDetailResponse;
import com.swyp.picke.domain.classroom.entity.ClassMember;
import com.swyp.picke.domain.classroom.entity.Classroom;
import com.swyp.picke.domain.classroom.enums.ClassBattleType;
import com.swyp.picke.domain.classroom.enums.ClassMemberRole;
import com.swyp.picke.domain.classroom.enums.ClassMemberStatus;
import com.swyp.picke.domain.classroom.enums.ClassStatus;
import com.swyp.picke.domain.classroom.repository.ClassMemberRepository;
import com.swyp.picke.domain.classroom.repository.ClassroomRepository;
import com.swyp.picke.domain.ticket.entity.ClassTicket;
import com.swyp.picke.domain.ticket.entity.FundingOrder;
import com.swyp.picke.domain.ticket.enums.FundingPlatform;
import com.swyp.picke.domain.ticket.enums.PaymentStatus;
import com.swyp.picke.domain.ticket.enums.RewardTier;
import com.swyp.picke.domain.ticket.enums.TicketStatus;
import com.swyp.picke.domain.ticket.repository.ClassTicketRepository;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.enums.UserRole;
import com.swyp.picke.domain.user.enums.UserStatus;
import com.swyp.picke.domain.user.repository.UserRepository;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassroomServiceImplTest {

    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_ID = 2L;
    private static final Long BATTLE_ID = 10L;
    private static final Long CLASS_ID = 100L;

    @Mock
    private ClassroomRepository classroomRepository;
    @Mock
    private ClassMemberRepository classMemberRepository;
    @Mock
    private ClassTicketRepository classTicketRepository;
    @Mock
    private BattleRepository battleRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ClassroomServiceImpl service;

    private User owner;
    private ClassTicket ticket;
    private Battle battle;

    @BeforeEach
    void setUp() {
        owner = user(OWNER_ID, "선생님");
        ticket = activeTicket(owner, LocalDateTime.now().minusDays(1));
        battle = battle(BattleStatus.PUBLISHED);
    }

    // ===== 생성 =====

    @Test
    @DisplayName("사용 중 이용권 + 공개 배틀이면 클래스가 만들어지고, 운영자 참여 시 OWNER 멤버 행이 생긴다")
    void create_success_withOwnerParticipating() {
        givenOwnerTicketBattle();
        when(classroomRepository.existsByInviteCode(anyString())).thenReturn(false);
        when(classroomRepository.save(any(Classroom.class))).thenAnswer(inv -> withId(inv.getArgument(0), CLASS_ID));

        ClassDetailResponse response = service.create(OWNER_ID,
                new ClassCreateRequest(" 3학년 2반 ", BATTLE_ID, null, null, true, true, null));

        ArgumentCaptor<Classroom> captor = ArgumentCaptor.forClass(Classroom.class);
        verify(classroomRepository).save(captor.capture());
        Classroom saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("3학년 2반");
        assertThat(saved.getInviteCode()).hasSize(Classroom.INVITE_CODE_LENGTH);
        assertThat(saved.getCapacity()).isEqualTo(5);
        assertThat(saved.getBattleType()).isEqualTo(ClassBattleType.TTS);
        assertThat(saved.getStatus()).isEqualTo(ClassStatus.ACTIVE);

        ArgumentCaptor<ClassMember> memberCaptor = ArgumentCaptor.forClass(ClassMember.class);
        verify(classMemberRepository).save(memberCaptor.capture());
        ClassMember member = memberCaptor.getValue();
        assertThat(member.getRole()).isEqualTo(ClassMemberRole.OWNER);
        assertThat(member.getDisplayName()).isEqualTo("선생님");
        assertThat(member.isSeatHeld()).isTrue();

        assertThat(response.myRole()).isEqualTo(ClassMemberRole.OWNER);
        assertThat(response.inviteCode()).isEqualTo(saved.getInviteCode());
        assertThat(response.memberCount()).isEqualTo(1);
        assertThat(response.closeAt()).isEqualTo(ticket.getExpiresAt());
        assertThat(response.closed()).isFalse();
    }

    @Test
    @DisplayName("운영자가 참여하지 않으면 멤버 행을 만들지 않는다")
    void create_withoutOwnerParticipating_noMemberRow() {
        givenOwnerTicketBattle();
        when(classroomRepository.existsByInviteCode(anyString())).thenReturn(false);
        when(classroomRepository.save(any(Classroom.class))).thenAnswer(inv -> withId(inv.getArgument(0), CLASS_ID));

        ClassDetailResponse response = service.create(OWNER_ID,
                new ClassCreateRequest("클래스", BATTLE_ID, null, null, false, false, null));

        verify(classMemberRepository, never()).save(any());
        assertThat(response.memberCount()).isZero();
        assertThat(response.ownerParticipates()).isFalse();
    }

    @Test
    @DisplayName("운영자 표시 이름을 주면 그 이름으로 멤버 행을 만든다")
    void create_usesRequestedOwnerDisplayName() {
        givenOwnerTicketBattle();
        when(classroomRepository.existsByInviteCode(anyString())).thenReturn(false);
        when(classroomRepository.save(any(Classroom.class))).thenAnswer(inv -> withId(inv.getArgument(0), CLASS_ID));

        service.create(OWNER_ID, new ClassCreateRequest("클래스", BATTLE_ID, null, null, false, true, " 김선생 "));

        ArgumentCaptor<ClassMember> captor = ArgumentCaptor.forClass(ClassMember.class);
        verify(classMemberRepository).save(captor.capture());
        assertThat(captor.getValue().getDisplayName()).isEqualTo("김선생");
    }

    @Test
    @DisplayName("초대코드가 겹치면 다시 뽑는다")
    void create_retriesInviteCodeOnCollision() {
        givenOwnerTicketBattle();
        when(classroomRepository.existsByInviteCode(anyString())).thenReturn(true, true, false);
        when(classroomRepository.save(any(Classroom.class))).thenAnswer(inv -> withId(inv.getArgument(0), CLASS_ID));

        service.create(OWNER_ID, new ClassCreateRequest("클래스", BATTLE_ID, null, null, false, false, null));

        verify(classroomRepository, times(3)).existsByInviteCode(anyString());
    }

    @Test
    @DisplayName("사용 중 이용권이 없으면 CLASS_403_TICKET")
    void create_rejectsWithoutActiveTicket() {
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(classTicketRepository.findByRegisteredUserIdAndStatus(OWNER_ID, TicketStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(OWNER_ID,
                new ClassCreateRequest("클래스", BATTLE_ID, null, null, false, false, null)))
                .isInstanceOf(CustomException.class)
                .extracting(ClassroomServiceImplTest::errorCodeOf)
                .isEqualTo(ErrorCode.CLASS_TICKET_REQUIRED);
        verify(classroomRepository, never()).save(any());
    }

    @Test
    @DisplayName("기간이 끝난 ACTIVE 이용권으로는 만들 수 없고(CLASS_403_TICKET), 이용권 상태는 건드리지 않는다")
    void create_rejectsStaleTicketWithoutTouchingIt() {
        ClassTicket stale = activeTicket(owner, LocalDateTime.now().minusMonths(7));
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(classTicketRepository.findByRegisteredUserIdAndStatus(OWNER_ID, TicketStatus.ACTIVE))
                .thenReturn(Optional.of(stale));

        assertThatThrownBy(() -> service.create(OWNER_ID,
                new ClassCreateRequest("클래스", BATTLE_ID, null, null, false, false, null)))
                .isInstanceOf(CustomException.class)
                .extracting(ClassroomServiceImplTest::errorCodeOf)
                .isEqualTo(ErrorCode.CLASS_TICKET_REQUIRED);
        assertThat(stale.getStatus()).isEqualTo(TicketStatus.ACTIVE);
    }

    @Test
    @DisplayName("공개되지 않은 배틀은 CLASS_400_BATTLE")
    void create_rejectsUnpublishedBattle() {
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(classTicketRepository.findByRegisteredUserIdAndStatus(OWNER_ID, TicketStatus.ACTIVE))
                .thenReturn(Optional.of(ticket));
        when(battleRepository.findById(BATTLE_ID)).thenReturn(Optional.of(battle(BattleStatus.PENDING)));

        assertThatThrownBy(() -> service.create(OWNER_ID,
                new ClassCreateRequest("클래스", BATTLE_ID, null, null, false, false, null)))
                .isInstanceOf(CustomException.class)
                .extracting(ClassroomServiceImplTest::errorCodeOf)
                .isEqualTo(ErrorCode.CLASS_BATTLE_NOT_AVAILABLE);
    }

    @Test
    @DisplayName("마감일이 이용권 종료일 이후거나 과거면 CLASS_400_DEADLINE")
    void create_rejectsDeadlineOutOfRange() {
        givenOwnerTicketBattle();
        LocalDateTime afterTicket = ticket.getExpiresAt().plusDays(1);
        LocalDateTime past = LocalDateTime.now().minusHours(1);

        for (LocalDateTime bad : new LocalDateTime[]{afterTicket, past}) {
            assertThatThrownBy(() -> service.create(OWNER_ID,
                    new ClassCreateRequest("클래스", BATTLE_ID, null, bad, false, false, null)))
                    .isInstanceOf(CustomException.class)
                    .extracting(ClassroomServiceImplTest::errorCodeOf)
                    .isEqualTo(ErrorCode.CLASS_INVALID_DEADLINE);
        }
        verify(classroomRepository, never()).save(any());
    }

    @Test
    @DisplayName("마감일이 있으면 실제 마감 = min(마감일, 이용권 종료일)")
    void create_withDeadline_closeAtIsDeadline() {
        givenOwnerTicketBattle();
        when(classroomRepository.existsByInviteCode(anyString())).thenReturn(false);
        when(classroomRepository.save(any(Classroom.class))).thenAnswer(inv -> withId(inv.getArgument(0), CLASS_ID));
        LocalDateTime deadline = LocalDateTime.now().plusDays(30);

        ClassDetailResponse response = service.create(OWNER_ID,
                new ClassCreateRequest("클래스", BATTLE_ID, null, deadline, false, false, null));

        assertThat(response.deadline()).isEqualTo(deadline);
        assertThat(response.closeAt()).isEqualTo(deadline);
    }

    // ===== 상세 =====

    @Test
    @DisplayName("운영자도 멤버도 아니면 상세를 볼 수 없다 (CLASS_403_MEMBER)")
    void getDetail_rejectsOutsider() {
        Classroom classroom = classroom(null);
        when(classroomRepository.findByIdAndStatus(CLASS_ID, ClassStatus.ACTIVE)).thenReturn(Optional.of(classroom));
        when(classMemberRepository.findByClassroomIdAndUserIdAndStatus(CLASS_ID, OTHER_ID, ClassMemberStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDetail(OTHER_ID, CLASS_ID))
                .isInstanceOf(CustomException.class)
                .extracting(ClassroomServiceImplTest::errorCodeOf)
                .isEqualTo(ErrorCode.CLASS_MEMBER_ONLY);
    }

    @Test
    @DisplayName("멤버도 공유하기용 초대코드를 받는다")
    void getDetail_memberSeesInviteCode() {
        Classroom classroom = classroom(null);
        User other = user(OTHER_ID, "학생");
        ClassMember member = ClassMember.builder().classroom(classroom).user(other).displayName("학생")
                .role(ClassMemberRole.MEMBER).joinedAt(LocalDateTime.now()).build();
        when(classroomRepository.findByIdAndStatus(CLASS_ID, ClassStatus.ACTIVE)).thenReturn(Optional.of(classroom));
        when(classMemberRepository.findByClassroomIdAndUserIdAndStatus(CLASS_ID, OTHER_ID, ClassMemberStatus.ACTIVE))
                .thenReturn(Optional.of(member));
        when(classMemberRepository.countByClassroomIdAndSeatHeldTrue(CLASS_ID)).thenReturn(2L);

        ClassDetailResponse response = service.getDetail(OTHER_ID, CLASS_ID);

        assertThat(response.myRole()).isEqualTo(ClassMemberRole.MEMBER);
        assertThat(response.inviteCode()).isEqualTo("ABC234");
        assertThat(response.memberCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("삭제된 클래스는 CLASS_404")
    void getDetail_deletedIsNotFound() {
        when(classroomRepository.findByIdAndStatus(CLASS_ID, ClassStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDetail(OWNER_ID, CLASS_ID))
                .isInstanceOf(CustomException.class)
                .extracting(ClassroomServiceImplTest::errorCodeOf)
                .isEqualTo(ErrorCode.CLASS_NOT_FOUND);
    }

    // ===== 수정 / 삭제 =====

    @Test
    @DisplayName("운영자가 아니면 수정·삭제할 수 없다 (CLASS_403_OWNER)")
    void update_rejectsNonOwner() {
        Classroom classroom = classroom(null);
        when(classroomRepository.findByIdAndStatus(CLASS_ID, ClassStatus.ACTIVE)).thenReturn(Optional.of(classroom));

        assertThatThrownBy(() -> service.update(OTHER_ID, CLASS_ID, new ClassUpdateRequest("새 이름", null)))
                .isInstanceOf(CustomException.class)
                .extracting(ClassroomServiceImplTest::errorCodeOf)
                .isEqualTo(ErrorCode.CLASS_OWNER_ONLY);
        assertThatThrownBy(() -> service.delete(OTHER_ID, CLASS_ID))
                .isInstanceOf(CustomException.class)
                .extracting(ClassroomServiceImplTest::errorCodeOf)
                .isEqualTo(ErrorCode.CLASS_OWNER_ONLY);
        assertThat(classroom.getName()).isEqualTo("클래스");
        assertThat(classroom.getStatus()).isEqualTo(ClassStatus.ACTIVE);
    }

    @Test
    @DisplayName("마감된 클래스는 수정할 수 없다 (CLASS_409_CLOSED)")
    void update_rejectsClosedClass() {
        Classroom classroom = classroom(LocalDateTime.now().minusMinutes(1));
        when(classroomRepository.findByIdAndStatus(CLASS_ID, ClassStatus.ACTIVE)).thenReturn(Optional.of(classroom));

        assertThatThrownBy(() -> service.update(OWNER_ID, CLASS_ID, new ClassUpdateRequest("새 이름", null)))
                .isInstanceOf(CustomException.class)
                .extracting(ClassroomServiceImplTest::errorCodeOf)
                .isEqualTo(ErrorCode.CLASS_CLOSED);
    }

    @Test
    @DisplayName("이름 변경과 마감일 연장이 반영된다")
    void update_renamesAndExtendsDeadline() {
        LocalDateTime current = LocalDateTime.now().plusDays(10);
        Classroom classroom = classroom(current);
        when(classroomRepository.findByIdAndStatus(CLASS_ID, ClassStatus.ACTIVE)).thenReturn(Optional.of(classroom));
        when(classMemberRepository.countByClassroomIdAndSeatHeldTrue(CLASS_ID)).thenReturn(0L);
        LocalDateTime extended = current.plusDays(5);

        ClassDetailResponse response = service.update(OWNER_ID, CLASS_ID, new ClassUpdateRequest(" 새 이름 ", extended));

        assertThat(classroom.getName()).isEqualTo("새 이름");
        assertThat(classroom.getDeadline()).isEqualTo(extended);
        assertThat(response.closeAt()).isEqualTo(extended);
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("마감일은 연장만 된다: 기존보다 앞당기거나, 마감 없음에서 새로 두면 CLASS_400_DEADLINE_EXT")
    void update_rejectsDeadlineNotExtended() {
        LocalDateTime current = LocalDateTime.now().plusDays(10);
        Classroom withDeadline = classroom(current);
        Classroom noDeadline = classroom(null);
        when(classroomRepository.findByIdAndStatus(CLASS_ID, ClassStatus.ACTIVE))
                .thenReturn(Optional.of(withDeadline), Optional.of(noDeadline));

        assertThatThrownBy(() -> service.update(OWNER_ID, CLASS_ID, new ClassUpdateRequest(null, current.minusDays(1))))
                .isInstanceOf(CustomException.class)
                .extracting(ClassroomServiceImplTest::errorCodeOf)
                .isEqualTo(ErrorCode.CLASS_DEADLINE_NOT_EXTENDED);
        assertThatThrownBy(() -> service.update(OWNER_ID, CLASS_ID, new ClassUpdateRequest(null, current)))
                .isInstanceOf(CustomException.class)
                .extracting(ClassroomServiceImplTest::errorCodeOf)
                .isEqualTo(ErrorCode.CLASS_DEADLINE_NOT_EXTENDED);
        assertThat(withDeadline.getDeadline()).isEqualTo(current);
        assertThat(noDeadline.getDeadline()).isNull();
    }

    @Test
    @DisplayName("연장한 마감일도 이용권 종료일을 넘길 수 없다 (CLASS_400_DEADLINE)")
    void update_rejectsDeadlineBeyondTicket() {
        Classroom classroom = classroom(LocalDateTime.now().plusDays(10));
        when(classroomRepository.findByIdAndStatus(CLASS_ID, ClassStatus.ACTIVE)).thenReturn(Optional.of(classroom));

        assertThatThrownBy(() -> service.update(OWNER_ID, CLASS_ID,
                new ClassUpdateRequest(null, ticket.getExpiresAt().plusDays(1))))
                .isInstanceOf(CustomException.class)
                .extracting(ClassroomServiceImplTest::errorCodeOf)
                .isEqualTo(ErrorCode.CLASS_INVALID_DEADLINE);
    }

    @Test
    @DisplayName("삭제하면 DELETED 상태와 삭제 시각이 남는다 (소프트 삭제)")
    void delete_softDeletes() {
        Classroom classroom = classroom(null);
        when(classroomRepository.findByIdAndStatus(CLASS_ID, ClassStatus.ACTIVE)).thenReturn(Optional.of(classroom));

        service.delete(OWNER_ID, CLASS_ID);

        assertThat(classroom.getStatus()).isEqualTo(ClassStatus.DELETED);
        assertThat(classroom.getDeletedAt()).isNotNull();
    }

    // ===== 픽스처 =====

    @Test
    @DisplayName("이용권이 환불(REVOKED)되면 기간이 남아 있어도 클래스는 마감이고 수정할 수 없다 (CLASS_409_CLOSED)")
    void update_rejectsWhenTicketRevoked() {
        LocalDateTime revokedAt = LocalDateTime.now().minusHours(1);
        ticket.revoke(revokedAt);
        Classroom classroom = classroom(LocalDateTime.now().plusDays(10));
        when(classroomRepository.findByIdAndStatus(CLASS_ID, ClassStatus.ACTIVE)).thenReturn(Optional.of(classroom));

        assertThat(classroom.isClosed(LocalDateTime.now())).isTrue();
        assertThat(classroom.getEffectiveCloseAt()).isEqualTo(revokedAt);
        assertThatThrownBy(() -> service.update(OWNER_ID, CLASS_ID, new ClassUpdateRequest("새 이름", null)))
                .isInstanceOf(CustomException.class)
                .extracting(ClassroomServiceImplTest::errorCodeOf)
                .isEqualTo(ErrorCode.CLASS_CLOSED);
    }

    private void givenOwnerTicketBattle() {
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(classTicketRepository.findByRegisteredUserIdAndStatus(OWNER_ID, TicketStatus.ACTIVE))
                .thenReturn(Optional.of(ticket));
        when(battleRepository.findById(BATTLE_ID)).thenReturn(Optional.of(battle));
    }

    private Classroom classroom(LocalDateTime deadline) {
        Classroom classroom = Classroom.builder()
                .name("클래스").inviteCode("ABC234").owner(owner).ticket(ticket).battle(battle)
                .battleType(ClassBattleType.TTS).capacity(5).deadline(deadline)
                .commentRequired(false).ownerParticipates(false)
                .build();
        return withId(classroom, CLASS_ID);
    }

    private static User user(Long id, String nickname) {
        User user = User.builder().userTag("u-" + id).nickname(nickname)
                .role(UserRole.USER).status(UserStatus.ACTIVE).build();
        return withId(user, id);
    }

    /** activatedAt 에 등록한 5인권 (만료 = activatedAt + 6개월) */
    private static ClassTicket activeTicket(User user, LocalDateTime activatedAt) {
        FundingOrder order = FundingOrder.builder()
                .platform(FundingPlatform.TUMBLBUG).campaignId("camp").orderId("order-" + activatedAt)
                .rewardTier(RewardTier.SEAT_5).quantity(1).paymentStatus(PaymentStatus.PAID_CONFIRMED)
                .paidAt(activatedAt.minusDays(1)).build();
        ClassTicket ticket = ClassTicket.issue(order, "hash-" + activatedAt);
        ticket.register(user, activatedAt);
        return withId(ticket, 50L);
    }

    private static Battle battle(BattleStatus status) {
        Battle battle = Battle.builder()
                .title("배틀").summary("요약").description("설명").thumbnailUrl("https://img/1.png")
                .targetDate(LocalDate.now()).status(status).creatorType(BattleCreatorType.ADMIN)
                .build();
        return withId(battle, BATTLE_ID);
    }

    private static <T> T withId(T entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    private static ErrorCode errorCodeOf(Throwable e) {
        return ((CustomException) e).getErrorCode();
    }
}

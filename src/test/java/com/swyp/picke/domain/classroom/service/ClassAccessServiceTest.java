package com.swyp.picke.domain.classroom.service;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.enums.BattleCreatorType;
import com.swyp.picke.domain.battle.enums.BattleStatus;
import com.swyp.picke.domain.classroom.entity.ClassMember;
import com.swyp.picke.domain.classroom.entity.Classroom;
import com.swyp.picke.domain.classroom.enums.ClassBattleType;
import com.swyp.picke.domain.classroom.enums.ClassMemberRole;
import com.swyp.picke.domain.classroom.enums.ClassMemberStatus;
import com.swyp.picke.domain.classroom.enums.ClassStatus;
import com.swyp.picke.domain.classroom.repository.ClassMemberRepository;
import com.swyp.picke.domain.classroom.repository.ClassroomRepository;
import com.swyp.picke.domain.classroom.service.ClassAccessService.ClassAccess;
import com.swyp.picke.domain.ticket.entity.ClassTicket;
import com.swyp.picke.domain.ticket.entity.FundingOrder;
import com.swyp.picke.domain.ticket.enums.FundingPlatform;
import com.swyp.picke.domain.ticket.enums.PaymentStatus;
import com.swyp.picke.domain.ticket.enums.RewardTier;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.enums.UserRole;
import com.swyp.picke.domain.user.enums.UserStatus;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassAccessServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final Long STUDENT_ID = 2L;
    private static final Long STRANGER_ID = 3L;
    private static final Long CLASS_ID = 100L;

    @Mock
    private ClassroomRepository classroomRepository;
    @Mock
    private ClassMemberRepository classMemberRepository;

    @InjectMocks
    private ClassAccessService service;

    private User owner;
    private User student;
    private ClassTicket ticket;
    private Classroom classroom;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
        owner = user(OWNER_ID, "선생님");
        student = user(STUDENT_ID, "학생");
        classroom = classroom(owner, null);
    }

    @Test
    @DisplayName("참여하지 않는 운영자: 읽기는 OWNER 역할로 통과, 쓰기는 CLASS_MEMBER_ONLY")
    void owner_withoutParticipation_canReadButNotWrite() {
        when(classroomRepository.findByIdAndStatus(CLASS_ID, ClassStatus.ACTIVE)).thenReturn(Optional.of(classroom));
        when(classMemberRepository.findByClassroomIdAndUserIdAndStatus(CLASS_ID, OWNER_ID, ClassMemberStatus.ACTIVE))
                .thenReturn(Optional.empty());

        ClassAccess access = service.requireReadable(CLASS_ID, OWNER_ID);
        assertThat(access.role()).isEqualTo(ClassMemberRole.OWNER);
        assertThat(access.canWrite()).isFalse();

        assertThatThrownBy(() -> service.requireWritable(CLASS_ID, OWNER_ID, now))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.CLASS_MEMBER_ONLY);
    }

    @Test
    @DisplayName("참여 중인 운영자(ownerParticipates): 역할은 OWNER 이고 쓰기도 가능")
    void owner_withParticipation_canWrite() {
        ClassMember ownerRow = member(owner, ClassMemberRole.OWNER);
        when(classroomRepository.findByIdAndStatus(CLASS_ID, ClassStatus.ACTIVE)).thenReturn(Optional.of(classroom));
        when(classMemberRepository.findByClassroomIdAndUserIdAndStatus(CLASS_ID, OWNER_ID, ClassMemberStatus.ACTIVE))
                .thenReturn(Optional.of(ownerRow));

        ClassAccess access = service.requireWritable(CLASS_ID, OWNER_ID, now);
        assertThat(access.role()).isEqualTo(ClassMemberRole.OWNER);
        assertThat(access.member()).isSameAs(ownerRow);
    }

    @Test
    @DisplayName("ACTIVE 멤버는 읽기·쓰기 모두 가능, 외부인은 읽기부터 CLASS_MEMBER_ONLY")
    void member_vs_stranger() {
        when(classroomRepository.findByIdAndStatus(CLASS_ID, ClassStatus.ACTIVE)).thenReturn(Optional.of(classroom));
        when(classMemberRepository.findByClassroomIdAndUserIdAndStatus(CLASS_ID, STUDENT_ID, ClassMemberStatus.ACTIVE))
                .thenReturn(Optional.of(member(student, ClassMemberRole.MEMBER)));
        when(classMemberRepository.findByClassroomIdAndUserIdAndStatus(CLASS_ID, STRANGER_ID, ClassMemberStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThat(service.requireWritable(CLASS_ID, STUDENT_ID, now).role()).isEqualTo(ClassMemberRole.MEMBER);
        assertThatThrownBy(() -> service.requireReadable(CLASS_ID, STRANGER_ID))
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.CLASS_MEMBER_ONLY);
    }

    @Test
    @DisplayName("마감일이 지나면 읽기는 되고 쓰기는 CLASS_CLOSED, 이용권이 회수되면 CLASS_TICKET_ENDED 가 우선")
    void closed_and_ticketEnded() {
        classroom = classroom(owner, now.minusMinutes(1));
        when(classroomRepository.findByIdAndStatus(CLASS_ID, ClassStatus.ACTIVE)).thenReturn(Optional.of(classroom));
        when(classMemberRepository.findByClassroomIdAndUserIdAndStatus(CLASS_ID, STUDENT_ID, ClassMemberStatus.ACTIVE))
                .thenReturn(Optional.of(member(student, ClassMemberRole.MEMBER)));

        assertThat(service.requireReadable(CLASS_ID, STUDENT_ID).canWrite()).isTrue();
        assertThatThrownBy(() -> service.requireWritable(CLASS_ID, STUDENT_ID, now))
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.CLASS_CLOSED);

        ticket.revoke(now.minusMinutes(2));
        assertThatThrownBy(() -> service.requireWritable(CLASS_ID, STUDENT_ID, now))
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.CLASS_TICKET_ENDED);
    }

    @Test
    @DisplayName("삭제된 클래스는 Classroom 객체로 들어와도 CLASS_NOT_FOUND")
    void deletedClassroom_notFound() {
        classroom.delete(now);
        assertThatThrownBy(() -> service.requireReadable(classroom, STUDENT_ID))
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.CLASS_NOT_FOUND);
    }

    // ===== 헬퍼 =====

    private ClassMember member(User user, ClassMemberRole role) {
        return ClassMember.builder()
                .classroom(classroom).user(user).displayName(user.getNickname()).role(role)
                .joinedAt(now.minusHours(1))
                .build();
    }

    private Classroom classroom(User owner, LocalDateTime deadline) {
        FundingOrder order = FundingOrder.builder()
                .platform(FundingPlatform.TUMBLBUG).campaignId("camp").orderId("order-1")
                .rewardTier(RewardTier.SEAT_5).quantity(1).paymentStatus(PaymentStatus.PAID_CONFIRMED)
                .paidAt(LocalDateTime.now().minusDays(2)).build();
        ticket = ClassTicket.issue(order, "hash-1");
        ticket.register(owner, LocalDateTime.now().minusDays(1));
        Battle battle = Battle.builder()
                .title("배틀").summary("요약").description("설명").thumbnailUrl("https://img/1.png")
                .targetDate(LocalDate.now()).status(BattleStatus.PUBLISHED).creatorType(BattleCreatorType.ADMIN)
                .build();
        withId(battle, 10L);
        Classroom c = Classroom.builder()
                .name("클래스").inviteCode("ABC234").owner(owner).ticket(ticket).battle(battle)
                .battleType(ClassBattleType.TTS).capacity(5).deadline(deadline)
                .commentRequired(true).ownerParticipates(false)
                .build();
        return withId(c, CLASS_ID);
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

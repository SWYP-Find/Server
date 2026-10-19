package com.swyp.picke.domain.classroom.service;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.enums.BattleCreatorType;
import com.swyp.picke.domain.battle.enums.BattleStatus;
import com.swyp.picke.domain.classroom.dto.request.ClassJoinRequest;
import com.swyp.picke.domain.classroom.dto.request.ClassMemberNameUpdateRequest;
import com.swyp.picke.domain.classroom.dto.response.ClassDetailResponse;
import com.swyp.picke.domain.classroom.dto.response.ClassMemberResponse;
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
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.enums.UserRole;
import com.swyp.picke.domain.user.enums.UserStatus;
import com.swyp.picke.domain.user.repository.UserRepository;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassMemberServiceImplTest {

    private static final Long OWNER_ID = 1L;
    private static final Long STUDENT_ID = 2L;
    private static final Long CLASS_ID = 100L;
    private static final String CODE = "ABC234";

    @Mock
    private ClassroomRepository classroomRepository;
    @Mock
    private ClassMemberRepository classMemberRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ClassMemberServiceImpl service;

    private User owner;
    private User student;
    private Classroom classroom;

    @BeforeEach
    void setUp() {
        owner = user(OWNER_ID, "선생님");
        student = user(STUDENT_ID, "학생");
        classroom = classroom(owner, 5, null);
    }

    // ===== 참여 =====

    @Test
    @DisplayName("코드는 공백 제거·대문자 변환되고, 정원이 남아 있으면 MEMBER 행이 생긴다")
    void join_success_normalizesCode() {
        givenClassByCode();
        when(classMemberRepository.findByClassroomIdAndUserId(CLASS_ID, STUDENT_ID)).thenReturn(Optional.empty());
        when(classMemberRepository.existsByClassroomIdAndDisplayNameAndStatus(CLASS_ID, "김학생", ClassMemberStatus.ACTIVE))
                .thenReturn(false);
        when(classMemberRepository.countByClassroomIdAndSeatHeldTrue(CLASS_ID)).thenReturn(4L, 5L);
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));

        ClassDetailResponse response = service.join(STUDENT_ID, new ClassJoinRequest(" abc-234 ", "  김학생 "));

        verify(classroomRepository).findByInviteCodeAndStatusForUpdate(CODE, ClassStatus.ACTIVE);
        ArgumentCaptor<ClassMember> captor = ArgumentCaptor.forClass(ClassMember.class);
        verify(classMemberRepository).save(captor.capture());
        ClassMember saved = captor.getValue();
        assertThat(saved.getDisplayName()).isEqualTo("김학생");
        assertThat(saved.getRole()).isEqualTo(ClassMemberRole.MEMBER);
        assertThat(saved.isSeatHeld()).isTrue();
        assertThat(saved.getUser()).isSameAs(student);

        assertThat(response.classId()).isEqualTo(CLASS_ID);
        assertThat(response.myRole()).isEqualTo(ClassMemberRole.MEMBER);
        assertThat(response.inviteCode()).isNull();
        assertThat(response.memberCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("정규화 후 6자가 아닌 코드와 빈 이름·21자 이름은 400 이고 DB 를 조회하지 않는다")
    void join_invalidCodeOrName_400() {
        assertThatThrownBy(() -> service.join(STUDENT_ID, new ClassJoinRequest("ABC23", "학생")))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo(ErrorCode.COMMON_INVALID_PARAMETER));
        assertThatThrownBy(() -> service.join(STUDENT_ID, new ClassJoinRequest(CODE, "   ")))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo(ErrorCode.COMMON_INVALID_PARAMETER));
        assertThatThrownBy(() -> service.join(STUDENT_ID, new ClassJoinRequest(CODE, "가".repeat(21))))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo(ErrorCode.COMMON_INVALID_PARAMETER));
        verify(classroomRepository, never()).findByInviteCodeAndStatusForUpdate(anyString(), any());
    }

    @Test
    @DisplayName("없는 코드(또는 삭제된 클래스)는 404 CLASS_404_CODE")
    void join_unknownCode_404() {
        when(classroomRepository.findByInviteCodeAndStatusForUpdate("ZZZZZZ", ClassStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.join(STUDENT_ID, new ClassJoinRequest("zzzzzz", "학생")))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo(ErrorCode.CLASS_INVITE_CODE_NOT_FOUND));
    }

    @Test
    @DisplayName("운영자가 자기 클래스 코드를 넣으면 409 CLASS_409_OWNER")
    void join_ownerOwnCode_409() {
        givenClassByCode();

        assertThatThrownBy(() -> service.join(OWNER_ID, new ClassJoinRequest(CODE, "선생님")))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo(ErrorCode.CLASS_OWNER_CANNOT_JOIN));
        verify(classMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("이미 참여 중이면 거절하지 않고 기존 상세를 돌려준다 (이름 변경·행 추가 없음)")
    void join_alreadyActive_returnsExisting() {
        givenClassByCode();
        ClassMember existing = member(student, "학생", ClassMemberRole.MEMBER);
        when(classMemberRepository.findByClassroomIdAndUserId(CLASS_ID, STUDENT_ID)).thenReturn(Optional.of(existing));
        when(classMemberRepository.countByClassroomIdAndSeatHeldTrue(CLASS_ID)).thenReturn(3L);

        ClassDetailResponse response = service.join(STUDENT_ID, new ClassJoinRequest(CODE, "다른이름"));

        assertThat(response.myRole()).isEqualTo(ClassMemberRole.MEMBER);
        assertThat(response.memberCount()).isEqualTo(3);
        assertThat(existing.getDisplayName()).isEqualTo("학생");
        verify(classMemberRepository, never()).save(any());
        verify(classMemberRepository, never()).existsByClassroomIdAndDisplayNameAndStatus(anyLong(), anyString(), any());
    }

    @Test
    @DisplayName("내보내진(REMOVED) 멤버의 재참여는 403 CLASS_403_REMOVED")
    void join_removed_403() {
        givenClassByCode();
        ClassMember removed = member(student, "학생", ClassMemberRole.MEMBER);
        removed.remove(LocalDateTime.now(), false);
        when(classMemberRepository.findByClassroomIdAndUserId(CLASS_ID, STUDENT_ID)).thenReturn(Optional.of(removed));

        assertThatThrownBy(() -> service.join(STUDENT_ID, new ClassJoinRequest(CODE, "학생")))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo(ErrorCode.CLASS_MEMBER_REMOVED));
        assertThat(removed.getStatus()).isEqualTo(ClassMemberStatus.REMOVED);
    }

    @Test
    @DisplayName("마감된 클래스는 409 CLASS_409_CLOSED")
    void join_closed_409() {
        classroom = classroom(owner, 5, LocalDateTime.now().minusMinutes(1));
        givenClassByCode();
        when(classMemberRepository.findByClassroomIdAndUserId(CLASS_ID, STUDENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.join(STUDENT_ID, new ClassJoinRequest(CODE, "학생")))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo(ErrorCode.CLASS_CLOSED));
        verify(classMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("이용권이 회수(REVOKED)되면 마감일이 남아 있어도 409 CLASS_409_TICKET_ENDED (입장 마감과 구분)")
    void join_ticketRevoked_409() {
        classroom = classroom(owner, 5, LocalDateTime.now().plusDays(7));
        classroom.getTicket().revoke(LocalDateTime.now().minusMinutes(1));
        givenClassByCode();
        when(classMemberRepository.findByClassroomIdAndUserId(CLASS_ID, STUDENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.join(STUDENT_ID, new ClassJoinRequest(CODE, "학생")))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo(ErrorCode.CLASS_TICKET_ENDED));
        verify(classMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("ACTIVE 멤버와 같은 이름이면 409 CLASS_409_NAME")
    void join_duplicateName_409() {
        givenClassByCode();
        when(classMemberRepository.findByClassroomIdAndUserId(CLASS_ID, STUDENT_ID)).thenReturn(Optional.empty());
        when(classMemberRepository.existsByClassroomIdAndDisplayNameAndStatus(CLASS_ID, "학생", ClassMemberStatus.ACTIVE))
                .thenReturn(true);

        assertThatThrownBy(() -> service.join(STUDENT_ID, new ClassJoinRequest(CODE, "학생")))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo(ErrorCode.CLASS_DISPLAY_NAME_DUPLICATED));
        verify(classMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("좌석(seatHeld) 수가 정원에 닿으면 409 CLASS_409_FULL")
    void join_full_409() {
        givenClassByCode();
        when(classMemberRepository.findByClassroomIdAndUserId(CLASS_ID, STUDENT_ID)).thenReturn(Optional.empty());
        when(classMemberRepository.existsByClassroomIdAndDisplayNameAndStatus(anyLong(), anyString(), any())).thenReturn(false);
        when(classMemberRepository.countByClassroomIdAndSeatHeldTrue(CLASS_ID)).thenReturn(5L);

        assertThatThrownBy(() -> service.join(STUDENT_ID, new ClassJoinRequest(CODE, "학생")))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo(ErrorCode.CLASS_FULL));
        verify(classMemberRepository, never()).save(any());
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("나갔던(LEFT) 멤버는 같은 행이 되살아나고, 좌석을 반환했었다면 정원 검사를 다시 받는다")
    void join_rejoinAfterLeave_reactivatesRow() {
        givenClassByCode();
        ClassMember left = member(student, "옛이름", ClassMemberRole.MEMBER);
        left.leave(LocalDateTime.now().minusDays(1), false);
        when(classMemberRepository.findByClassroomIdAndUserId(CLASS_ID, STUDENT_ID)).thenReturn(Optional.of(left));
        when(classMemberRepository.existsByClassroomIdAndDisplayNameAndStatus(CLASS_ID, "새이름", ClassMemberStatus.ACTIVE))
                .thenReturn(false);
        when(classMemberRepository.countByClassroomIdAndSeatHeldTrue(CLASS_ID)).thenReturn(4L, 5L);

        ClassDetailResponse response = service.join(STUDENT_ID, new ClassJoinRequest(CODE, "새이름"));

        assertThat(left.getStatus()).isEqualTo(ClassMemberStatus.ACTIVE);
        assertThat(left.getDisplayName()).isEqualTo("새이름");
        assertThat(left.isSeatHeld()).isTrue();
        assertThat(left.getLeftAt()).isNull();
        assertThat(response.memberCount()).isEqualTo(5);
        verify(classMemberRepository, never()).save(any());
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("활동 이력으로 좌석을 쥔 채 나간(LEFT, seatHeld) 멤버의 재참여는 정원이 꽉 차 있어도 된다")
    void join_rejoinWithSeatHeld_skipsCapacityCheck() {
        givenClassByCode();
        ClassMember left = member(student, "학생", ClassMemberRole.MEMBER);
        left.leave(LocalDateTime.now().minusDays(1), true);
        when(classMemberRepository.findByClassroomIdAndUserId(CLASS_ID, STUDENT_ID)).thenReturn(Optional.of(left));
        when(classMemberRepository.existsByClassroomIdAndDisplayNameAndStatus(anyLong(), anyString(), any())).thenReturn(false);
        when(classMemberRepository.countByClassroomIdAndSeatHeldTrue(CLASS_ID)).thenReturn(5L);

        ClassDetailResponse response = service.join(STUDENT_ID, new ClassJoinRequest(CODE, "학생"));

        assertThat(left.getStatus()).isEqualTo(ClassMemberStatus.ACTIVE);
        assertThat(response.memberCount()).isEqualTo(5);
    }

    // ===== 명단 =====

    @Test
    @DisplayName("명단은 운영자·멤버만 볼 수 있고 본인 행에 me=true, 계정 ID 는 내려가지 않는다")
    void getMembers_ownerAndMemberOnly() {
        when(classroomRepository.findByIdAndStatus(CLASS_ID, ClassStatus.ACTIVE)).thenReturn(Optional.of(classroom));
        ClassMember ownerRow = withId(member(owner, "선생님", ClassMemberRole.OWNER), 11L);
        ClassMember studentRow = withId(member(student, "학생", ClassMemberRole.MEMBER), 12L);
        when(classMemberRepository.findByClassroomIdAndUserIdAndStatus(CLASS_ID, STUDENT_ID, ClassMemberStatus.ACTIVE))
                .thenReturn(Optional.of(studentRow));
        when(classMemberRepository.findActiveMembers(CLASS_ID)).thenReturn(List.of(ownerRow, studentRow));

        List<ClassMemberResponse> members = service.getMembers(STUDENT_ID, CLASS_ID);

        assertThat(members).extracting(ClassMemberResponse::memberId).containsExactly(11L, 12L);
        assertThat(members).extracting(ClassMemberResponse::me).containsExactly(false, true);
        assertThat(members.get(0).role()).isEqualTo(ClassMemberRole.OWNER);

        when(classMemberRepository.findByClassroomIdAndUserIdAndStatus(CLASS_ID, 99L, ClassMemberStatus.ACTIVE))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getMembers(99L, CLASS_ID))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo(ErrorCode.CLASS_MEMBER_ONLY));
    }

    // ===== 이름 변경 =====

    @Test
    @DisplayName("내 이름 변경: 중복이면 409, 같은 이름 그대로면 중복 검사 없이 통과")
    void changeMyDisplayName() {
        when(classroomRepository.findByIdAndStatus(CLASS_ID, ClassStatus.ACTIVE)).thenReturn(Optional.of(classroom));
        ClassMember me = withId(member(student, "학생", ClassMemberRole.MEMBER), 12L);
        when(classMemberRepository.findByClassroomIdAndUserIdAndStatus(CLASS_ID, STUDENT_ID, ClassMemberStatus.ACTIVE))
                .thenReturn(Optional.of(me));
        when(classMemberRepository.existsByClassroomIdAndDisplayNameAndStatus(CLASS_ID, "겹침", ClassMemberStatus.ACTIVE))
                .thenReturn(true);
        when(classMemberRepository.existsByClassroomIdAndDisplayNameAndStatus(CLASS_ID, "새이름", ClassMemberStatus.ACTIVE))
                .thenReturn(false);

        assertThatThrownBy(() -> service.changeMyDisplayName(STUDENT_ID, CLASS_ID, new ClassMemberNameUpdateRequest("겹침")))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo(ErrorCode.CLASS_DISPLAY_NAME_DUPLICATED));
        assertThat(me.getDisplayName()).isEqualTo("학생");

        ClassMemberResponse same = service.changeMyDisplayName(STUDENT_ID, CLASS_ID, new ClassMemberNameUpdateRequest(" 학생 "));
        assertThat(same.displayName()).isEqualTo("학생");

        ClassMemberResponse changed = service.changeMyDisplayName(STUDENT_ID, CLASS_ID, new ClassMemberNameUpdateRequest("새이름"));
        assertThat(changed.displayName()).isEqualTo("새이름");
        assertThat(changed.me()).isTrue();
        assertThat(me.getDisplayName()).isEqualTo("새이름");
    }

    @Test
    @DisplayName("참여하지 않은 유저(멤버 행 없는 운영자 포함)의 이름 변경은 403 CLASS_403_MEMBER")
    void changeMyDisplayName_notMember_403() {
        when(classroomRepository.findByIdAndStatus(CLASS_ID, ClassStatus.ACTIVE)).thenReturn(Optional.of(classroom));
        when(classMemberRepository.findByClassroomIdAndUserIdAndStatus(CLASS_ID, OWNER_ID, ClassMemberStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeMyDisplayName(OWNER_ID, CLASS_ID, new ClassMemberNameUpdateRequest("이름")))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo(ErrorCode.CLASS_MEMBER_ONLY));
    }

    // ===== 나가기 / 내보내기 =====

    @Test
    @DisplayName("나가기: 활동 전이라 LEFT + 좌석 반환(seatHeld=false), 운영자는 403 CLASS_403_OWNER_LEAVE")
    void leave() {
        when(classroomRepository.findByIdAndStatus(CLASS_ID, ClassStatus.ACTIVE)).thenReturn(Optional.of(classroom));
        ClassMember me = member(student, "학생", ClassMemberRole.MEMBER);
        when(classMemberRepository.findByClassroomIdAndUserIdAndStatus(CLASS_ID, STUDENT_ID, ClassMemberStatus.ACTIVE))
                .thenReturn(Optional.of(me));

        service.leave(STUDENT_ID, CLASS_ID);

        assertThat(me.getStatus()).isEqualTo(ClassMemberStatus.LEFT);
        assertThat(me.isSeatHeld()).isFalse();
        assertThat(me.getLeftAt()).isNotNull();

        assertThatThrownBy(() -> service.leave(OWNER_ID, CLASS_ID))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo(ErrorCode.CLASS_OWNER_CANNOT_LEAVE));
    }

    @Test
    @DisplayName("내보내기: 운영자만 가능(403 CLASS_403_OWNER), 대상은 REMOVED, 운영자 본인 행은 403, 없는 멤버는 404")
    void removeMember() {
        when(classroomRepository.findByIdAndStatus(CLASS_ID, ClassStatus.ACTIVE)).thenReturn(Optional.of(classroom));
        ClassMember target = withId(member(student, "학생", ClassMemberRole.MEMBER), 12L);
        ClassMember ownerRow = withId(member(owner, "선생님", ClassMemberRole.OWNER), 11L);
        when(classMemberRepository.findByIdAndClassroomId(12L, CLASS_ID)).thenReturn(Optional.of(target));
        when(classMemberRepository.findByIdAndClassroomId(11L, CLASS_ID)).thenReturn(Optional.of(ownerRow));
        when(classMemberRepository.findByIdAndClassroomId(99L, CLASS_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removeMember(STUDENT_ID, CLASS_ID, 12L))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo(ErrorCode.CLASS_OWNER_ONLY));
        assertThat(target.getStatus()).isEqualTo(ClassMemberStatus.ACTIVE);

        service.removeMember(OWNER_ID, CLASS_ID, 12L);
        assertThat(target.getStatus()).isEqualTo(ClassMemberStatus.REMOVED);
        assertThat(target.isSeatHeld()).isFalse();

        assertThatThrownBy(() -> service.removeMember(OWNER_ID, CLASS_ID, 11L))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo(ErrorCode.CLASS_OWNER_CANNOT_LEAVE));
        assertThat(ownerRow.getStatus()).isEqualTo(ClassMemberStatus.ACTIVE);

        assertThatThrownBy(() -> service.removeMember(OWNER_ID, CLASS_ID, 99L))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo(ErrorCode.CLASS_MEMBER_NOT_FOUND));
        // 이미 REMOVED 된 행을 다시 내보내면 404
        assertThatThrownBy(() -> service.removeMember(OWNER_ID, CLASS_ID, 12L))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo(ErrorCode.CLASS_MEMBER_NOT_FOUND));
    }

    // ===== 픽스처 =====

    private void givenClassByCode() {
        when(classroomRepository.findByInviteCodeAndStatusForUpdate(CODE, ClassStatus.ACTIVE))
                .thenReturn(Optional.of(classroom));
    }

    private ClassMember member(User user, String displayName, ClassMemberRole role) {
        return ClassMember.builder()
                .classroom(classroom).user(user).displayName(displayName).role(role)
                .joinedAt(LocalDateTime.now().minusHours(1))
                .build();
    }

    private static Classroom classroom(User owner, int capacity, LocalDateTime deadline) {
        FundingOrder order = FundingOrder.builder()
                .platform(FundingPlatform.TUMBLBUG).campaignId("camp").orderId("order-1")
                .rewardTier(RewardTier.SEAT_5).quantity(1).paymentStatus(PaymentStatus.PAID_CONFIRMED)
                .paidAt(LocalDateTime.now().minusDays(2)).build();
        ClassTicket ticket = ClassTicket.issue(order, "hash-1");
        ticket.register(owner, LocalDateTime.now().minusDays(1));
        Battle battle = Battle.builder()
                .title("배틀").summary("요약").description("설명").thumbnailUrl("https://img/1.png")
                .targetDate(LocalDate.now()).status(BattleStatus.PUBLISHED).creatorType(BattleCreatorType.ADMIN)
                .build();
        withId(battle, 10L);
        Classroom classroom = Classroom.builder()
                .name("클래스").inviteCode(CODE).owner(owner).ticket(ticket).battle(battle)
                .battleType(ClassBattleType.TTS).capacity(capacity).deadline(deadline)
                .commentRequired(false).ownerParticipates(false)
                .build();
        return withId(classroom, CLASS_ID);
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

    private static ErrorCode errorCodeOf(Throwable e) {
        return ((CustomException) e).getErrorCode();
    }
}

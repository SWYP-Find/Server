package com.swyp.picke.domain.ticket.service;

import com.swyp.picke.domain.ticket.dto.request.ClassTicketRegisterRequest;
import com.swyp.picke.domain.ticket.dto.response.ClassTicketPreviewResponse;
import com.swyp.picke.domain.ticket.dto.response.ClassTicketResponse;
import com.swyp.picke.domain.ticket.entity.ClassTicket;
import com.swyp.picke.domain.ticket.entity.FundingOrder;
import com.swyp.picke.domain.ticket.enums.FundingPlatform;
import com.swyp.picke.domain.ticket.enums.PaymentStatus;
import com.swyp.picke.domain.ticket.enums.RewardTier;
import com.swyp.picke.domain.ticket.enums.TicketCodeStatus;
import com.swyp.picke.domain.ticket.enums.TicketStatus;
import com.swyp.picke.domain.ticket.repository.ClassTicketRepository;
import com.swyp.picke.domain.ticket.util.TicketCodeHasher;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.enums.UserRole;
import com.swyp.picke.domain.user.enums.UserStatus;
import com.swyp.picke.domain.user.repository.UserRepository;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassTicketServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final String CODE = "PICKE-AB12-CD34";
    private static final String HASH = TicketCodeHasher.hash(CODE);

    @Mock
    private ClassTicketRepository classTicketRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ClassTicketServiceImpl service;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .userTag("u-0001")
                .nickname("tester")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
    }

    /** 결제 확정이 paidAt 인 5인권을 발급한다. registerBy = paidAt + 1년 */
    private ClassTicket issuedTicket(LocalDateTime paidAt) {
        FundingOrder order = FundingOrder.builder()
                .platform(FundingPlatform.TUMBLBUG)
                .campaignId("camp-1")
                .orderId("order-" + paidAt.toLocalDate())
                .rewardTier(RewardTier.SEAT_5)
                .quantity(1)
                .paymentStatus(PaymentStatus.PAID_CONFIRMED)
                .paidAt(paidAt)
                .build();
        return ClassTicket.issue(order, HASH);
    }

    private void givenUserAndCode(ClassTicket ticket) {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(classTicketRepository.findByCodeHashForUpdate(HASH)).thenReturn(Optional.of(ticket));
    }

    private User otherUser() {
        User other = User.builder().userTag("u-0002").nickname("other")
                .role(UserRole.USER).status(UserStatus.ACTIVE).build();
        ReflectionTestUtils.setField(other, "id", 2L);
        return other;
    }

    private static ErrorCode errorCodeOf(Throwable e) {
        return ((CustomException) e).getErrorCode();
    }

    @Test
    @DisplayName("유효한 코드를 등록하면 ACTIVE 가 되고 만료일은 등록 시점 + 6개월이다")
    void register_activatesTicket() {
        ClassTicket ticket = issuedTicket(LocalDateTime.now().minusDays(10));
        givenUserAndCode(ticket);
        when(classTicketRepository.findByRegisteredUserIdAndStatus(USER_ID, TicketStatus.ACTIVE))
                .thenReturn(Optional.empty());

        ClassTicketResponse response = service.register(USER_ID, new ClassTicketRegisterRequest(" picke-ab12-cd34 "));

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.ACTIVE);
        assertThat(ticket.getCodeStatus()).isEqualTo(TicketCodeStatus.CLAIMED);
        assertThat(ticket.getRegisteredUser()).isSameAs(user);
        assertThat(ticket.getExpiresAt()).isEqualTo(ticket.getActivatedAt().plusMonths(6));
        assertThat(response.capacity()).isEqualTo(5);
        assertThat(response.status()).isEqualTo(TicketStatus.ACTIVE);
        assertThat(response.remainingDays()).isBetween(180L, 184L);
        assertThat(response.readGraceEndsAt()).isEqualTo(ticket.getExpiresAt().plusDays(30));
    }

    @Test
    @DisplayName("존재하지 않는 코드는 TICKET_404_CODE")
    void register_rejectsUnknownCode() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(classTicketRepository.findByCodeHashForUpdate(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.register(USER_ID, new ClassTicketRegisterRequest("NOPE")))
                .isInstanceOf(CustomException.class)
                .extracting(ClassTicketServiceImplTest::errorCodeOf)
                .isEqualTo(ErrorCode.TICKET_CODE_INVALID);
    }

    @Test
    @DisplayName("이미 다른 계정이 등록한 코드는 TICKET_409_CLAIMED")
    void register_rejectsClaimedCode() {
        ClassTicket ticket = issuedTicket(LocalDateTime.now().minusDays(10));
        ticket.register(otherUser(), LocalDateTime.now().minusDays(1));
        givenUserAndCode(ticket);

        assertThatThrownBy(() -> service.register(USER_ID, new ClassTicketRegisterRequest(CODE)))
                .isInstanceOf(CustomException.class)
                .extracting(ClassTicketServiceImplTest::errorCodeOf)
                .isEqualTo(ErrorCode.TICKET_CODE_ALREADY_CLAIMED);
    }

    @Test
    @DisplayName("재발급으로 무효화된 코드는 TICKET_409_REVOKED")
    void register_rejectsRevokedCode() {
        ClassTicket ticket = issuedTicket(LocalDateTime.now().minusDays(10));
        ticket.revoke(LocalDateTime.now().minusDays(1));
        givenUserAndCode(ticket);

        assertThatThrownBy(() -> service.register(USER_ID, new ClassTicketRegisterRequest(CODE)))
                .isInstanceOf(CustomException.class)
                .extracting(ClassTicketServiceImplTest::errorCodeOf)
                .isEqualTo(ErrorCode.TICKET_CODE_REVOKED);
    }

    @Test
    @DisplayName("결제 후 1년이 지난 코드는 REGISTER_EXPIRED 로 바뀌고 TICKET_409_REG_EXP")
    void register_rejectsWhenRegisterWindowClosed() {
        ClassTicket ticket = issuedTicket(LocalDateTime.now().minusYears(1).minusDays(1));
        givenUserAndCode(ticket);

        assertThatThrownBy(() -> service.register(USER_ID, new ClassTicketRegisterRequest(CODE)))
                .isInstanceOf(CustomException.class)
                .extracting(ClassTicketServiceImplTest::errorCodeOf)
                .isEqualTo(ErrorCode.TICKET_REGISTER_EXPIRED);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.REGISTER_EXPIRED);
        assertThat(ticket.getCodeStatus()).isEqualTo(TicketCodeStatus.ISSUED);
    }

    @Test
    @DisplayName("사용 중인 이용권이 있으면 새 코드를 등록할 수 없다 (TICKET_409_ACTIVE)")
    void register_rejectsWhenUserAlreadyHasActiveTicket() {
        ClassTicket active = issuedTicket(LocalDateTime.now().minusDays(30));
        active.register(user, LocalDateTime.now().minusDays(20));
        ClassTicket newTicket = issuedTicket(LocalDateTime.now().minusDays(1));
        givenUserAndCode(newTicket);
        when(classTicketRepository.findByRegisteredUserIdAndStatus(USER_ID, TicketStatus.ACTIVE))
                .thenReturn(Optional.of(active));

        assertThatThrownBy(() -> service.register(USER_ID, new ClassTicketRegisterRequest(CODE)))
                .isInstanceOf(CustomException.class)
                .extracting(ClassTicketServiceImplTest::errorCodeOf)
                .isEqualTo(ErrorCode.TICKET_ALREADY_ACTIVE);
        assertThat(newTicket.getStatus()).isEqualTo(TicketStatus.UNREGISTERED);
    }

    @Test
    @DisplayName("기간이 끝난 ACTIVE 이용권은 EXPIRED 로 내리고 새 코드 등록을 허용한다")
    void register_expiresStaleActiveTicketAndProceeds() {
        ClassTicket stale = issuedTicket(LocalDateTime.now().minusMonths(8));
        stale.register(user, LocalDateTime.now().minusMonths(7));
        ClassTicket newTicket = issuedTicket(LocalDateTime.now().minusDays(1));
        givenUserAndCode(newTicket);
        when(classTicketRepository.findByRegisteredUserIdAndStatus(USER_ID, TicketStatus.ACTIVE))
                .thenReturn(Optional.of(stale));

        service.register(USER_ID, new ClassTicketRegisterRequest(CODE));

        assertThat(stale.getStatus()).isEqualTo(TicketStatus.EXPIRED);
        assertThat(newTicket.getStatus()).isEqualTo(TicketStatus.ACTIVE);
    }

    @Test
    @DisplayName("등록한 이용권이 없으면 TICKET_404")
    void getMyTicket_notFound() {
        when(classTicketRepository.findTopByRegisteredUserIdAndStatusInOrderByActivatedAtDesc(eq(USER_ID), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMyTicket(USER_ID))
                .isInstanceOf(CustomException.class)
                .extracting(ClassTicketServiceImplTest::errorCodeOf)
                .isEqualTo(ErrorCode.TICKET_NOT_FOUND);
    }

    @Test
    @DisplayName("만료 시각이 지난 ACTIVE 이용권은 조회 시 EXPIRED 로 바뀌고 잔여일 0 으로 나간다")
    void getMyTicket_lazilyExpires() {
        ClassTicket ticket = issuedTicket(LocalDateTime.now().minusMonths(8));
        ticket.register(user, LocalDateTime.now().minusMonths(7));
        when(classTicketRepository.findTopByRegisteredUserIdAndStatusInOrderByActivatedAtDesc(
                eq(USER_ID), eq(List.of(TicketStatus.ACTIVE, TicketStatus.EXPIRED))))
                .thenReturn(Optional.of(ticket));

        ClassTicketResponse response = service.getMyTicket(USER_ID);

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.EXPIRED);
        assertThat(response.status()).isEqualTo(TicketStatus.EXPIRED);
        assertThat(response.remainingDays()).isZero();
        assertThat(response.readGraceEndsAt()).isEqualTo(ticket.getExpiresAt().plusDays(30));
    }

    @Test
    @DisplayName("사용 중인 이용권은 정원과 잔여 일수를 돌려준다")
    void getMyTicket_active() {
        ClassTicket ticket = issuedTicket(LocalDateTime.now().minusDays(5));
        ticket.register(user, LocalDateTime.now().minusDays(1));
        when(classTicketRepository.findTopByRegisteredUserIdAndStatusInOrderByActivatedAtDesc(anyLong(), any()))
                .thenReturn(Optional.of(ticket));

        ClassTicketResponse response = service.getMyTicket(USER_ID);

        assertThat(response.status()).isEqualTo(TicketStatus.ACTIVE);
        assertThat(response.capacity()).isEqualTo(5);
        assertThat(response.remainingDays()).isBetween(178L, 183L);
    }

    // ---- #535 본인 코드 재등록 = 기존 결과 반환 (PRD §4)

    @Test
    @DisplayName("내가 이미 등록한 코드를 다시 등록하면 거절 대신 기존 이용권을 그대로 돌려준다 (기간 불변)")
    void register_ownCode_returnsExistingResult() {
        ReflectionTestUtils.setField(user, "id", USER_ID);
        ClassTicket ticket = issuedTicket(LocalDateTime.now().minusDays(10));
        LocalDateTime activatedAt = LocalDateTime.now().minusDays(3);
        ticket.register(user, activatedAt);
        givenUserAndCode(ticket);

        ClassTicketResponse response = service.register(USER_ID, new ClassTicketRegisterRequest(CODE));

        assertThat(response.status()).isEqualTo(TicketStatus.ACTIVE);
        assertThat(ticket.getActivatedAt()).isEqualTo(activatedAt);
        assertThat(ticket.getExpiresAt()).isEqualTo(activatedAt.plusMonths(6));
    }

    @Test
    @DisplayName("내 코드가 이미 만료됐으면 재등록해도 EXPIRED 결과를 돌려주고 기간을 되살리지 않는다")
    void register_ownExpiredCode_returnsExpired() {
        ReflectionTestUtils.setField(user, "id", USER_ID);
        ClassTicket ticket = issuedTicket(LocalDateTime.now().minusMonths(8));
        ticket.register(user, LocalDateTime.now().minusMonths(7));
        givenUserAndCode(ticket);

        ClassTicketResponse response = service.register(USER_ID, new ClassTicketRegisterRequest(CODE));

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.EXPIRED);
        assertThat(response.status()).isEqualTo(TicketStatus.EXPIRED);
        assertThat(response.remainingDays()).isZero();
    }

    @Test
    @DisplayName("내 코드라도 환불·재발급으로 REVOKED 됐으면 TICKET_409_REVOKED")
    void register_ownRevokedCode_rejected() {
        ReflectionTestUtils.setField(user, "id", USER_ID);
        ClassTicket ticket = issuedTicket(LocalDateTime.now().minusDays(10));
        ticket.register(user, LocalDateTime.now().minusDays(3));
        ticket.revoke(LocalDateTime.now().minusDays(1));
        givenUserAndCode(ticket);

        assertThatThrownBy(() -> service.register(USER_ID, new ClassTicketRegisterRequest(CODE)))
                .isInstanceOf(CustomException.class)
                .extracting(ClassTicketServiceImplTest::errorCodeOf)
                .isEqualTo(ErrorCode.TICKET_CODE_REVOKED);
    }

    // ---- #535 코드 미리보기 (소비 없음)

    @Test
    @DisplayName("미리보기는 정원·등록 기한을 돌려주고 코드 상태를 바꾸지 않는다")
    void preview_returnsCapacityAndRegisterBy() {
        LocalDateTime paidAt = LocalDateTime.now().minusDays(10);
        ClassTicket ticket = issuedTicket(paidAt);
        when(classTicketRepository.findByCodeHash(HASH)).thenReturn(Optional.of(ticket));
        when(classTicketRepository.findByRegisteredUserIdAndStatus(USER_ID, TicketStatus.ACTIVE))
                .thenReturn(Optional.empty());

        ClassTicketPreviewResponse response = service.preview(USER_ID, " picke-ab12-cd34 ");

        assertThat(response.capacity()).isEqualTo(5);
        assertThat(response.registerBy()).isEqualTo(paidAt.plusYears(1));
        assertThat(response.usageMonths()).isEqualTo(6);
        assertThat(response.registeredByMe()).isFalse();
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.UNREGISTERED);
        assertThat(ticket.getCodeStatus()).isEqualTo(TicketCodeStatus.ISSUED);
        assertThat(ticket.getRegisteredUser()).isNull();
    }

    @Test
    @DisplayName("미리보기: 등록 기한이 지난 코드는 TICKET_409_REG_EXP 이지만 상태는 UNREGISTERED 그대로다 (등록 때만 기록)")
    void preview_registerWindowClosed_doesNotMutate() {
        ClassTicket ticket = issuedTicket(LocalDateTime.now().minusYears(1).minusDays(1));
        when(classTicketRepository.findByCodeHash(HASH)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> service.preview(USER_ID, CODE))
                .isInstanceOf(CustomException.class)
                .extracting(ClassTicketServiceImplTest::errorCodeOf)
                .isEqualTo(ErrorCode.TICKET_REGISTER_EXPIRED);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.UNREGISTERED);
    }

    @Test
    @DisplayName("미리보기: 다른 계정이 등록한 코드는 TICKET_409_CLAIMED, 없는 코드는 TICKET_404_CODE")
    void preview_rejectsClaimedAndUnknown() {
        ClassTicket claimed = issuedTicket(LocalDateTime.now().minusDays(10));
        claimed.register(otherUser(), LocalDateTime.now().minusDays(1));
        when(classTicketRepository.findByCodeHash(HASH)).thenReturn(Optional.of(claimed));

        assertThatThrownBy(() -> service.preview(USER_ID, CODE))
                .extracting(ClassTicketServiceImplTest::errorCodeOf)
                .isEqualTo(ErrorCode.TICKET_CODE_ALREADY_CLAIMED);

        when(classTicketRepository.findByCodeHash(TicketCodeHasher.hash("NOPE"))).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.preview(USER_ID, "NOPE"))
                .extracting(ClassTicketServiceImplTest::errorCodeOf)
                .isEqualTo(ErrorCode.TICKET_CODE_INVALID);
    }

    @Test
    @DisplayName("미리보기: 사용 중인 이용권이 있으면 TICKET_409_ACTIVE 를 미리 알려준다")
    void preview_rejectsWhenUserHasActiveTicket() {
        ClassTicket active = issuedTicket(LocalDateTime.now().minusDays(30));
        active.register(user, LocalDateTime.now().minusDays(20));
        ClassTicket newTicket = issuedTicket(LocalDateTime.now().minusDays(1));
        when(classTicketRepository.findByCodeHash(HASH)).thenReturn(Optional.of(newTicket));
        when(classTicketRepository.findByRegisteredUserIdAndStatus(USER_ID, TicketStatus.ACTIVE))
                .thenReturn(Optional.of(active));

        assertThatThrownBy(() -> service.preview(USER_ID, CODE))
                .extracting(ClassTicketServiceImplTest::errorCodeOf)
                .isEqualTo(ErrorCode.TICKET_ALREADY_ACTIVE);
    }

    @Test
    @DisplayName("미리보기: 내가 등록한 코드면 registeredByMe=true 로 돌려준다")
    void preview_ownCode_registeredByMe() {
        ReflectionTestUtils.setField(user, "id", USER_ID);
        ClassTicket ticket = issuedTicket(LocalDateTime.now().minusDays(10));
        ticket.register(user, LocalDateTime.now().minusDays(3));
        when(classTicketRepository.findByCodeHash(HASH)).thenReturn(Optional.of(ticket));

        ClassTicketPreviewResponse response = service.preview(USER_ID, CODE);

        assertThat(response.registeredByMe()).isTrue();
        assertThat(response.capacity()).isEqualTo(5);
    }

    @Test
    @DisplayName("미리보기: 빈 코드는 COMMON_400")
    void preview_blankCode() {
        assertThatThrownBy(() -> service.preview(USER_ID, " - "))
                .extracting(ClassTicketServiceImplTest::errorCodeOf)
                .isEqualTo(ErrorCode.COMMON_INVALID_PARAMETER);
    }
}

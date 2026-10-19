package com.swyp.picke.domain.ticket.service;

import com.swyp.picke.domain.ticket.dto.request.ClassTicketRegisterRequest;
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
        ticket.register(user, LocalDateTime.now().minusDays(1));
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
}

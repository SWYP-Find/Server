package com.swyp.picke.domain.ticket.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swyp.picke.domain.oauth.jwt.JwtProvider;
import com.swyp.picke.domain.ticket.entity.ClassTicket;
import com.swyp.picke.domain.ticket.entity.FundingOrder;
import com.swyp.picke.domain.ticket.enums.FundingPlatform;
import com.swyp.picke.domain.ticket.enums.PaymentStatus;
import com.swyp.picke.domain.ticket.enums.RewardTier;
import com.swyp.picke.domain.ticket.enums.TicketStatus;
import com.swyp.picke.domain.ticket.repository.ClassTicketRepository;
import com.swyp.picke.domain.ticket.repository.FundingOrderRepository;
import com.swyp.picke.domain.ticket.util.TicketCodeHasher;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.enums.UserRole;
import com.swyp.picke.domain.user.enums.UserStatus;
import com.swyp.picke.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 펀딩 후원자가 받은 코드를 앱에서 등록하고 내 이용권을 확인하는 흐름을 HTTP 로 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClassTicketApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JwtProvider jwtProvider;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FundingOrderRepository fundingOrderRepository;
    @Autowired
    private ClassTicketRepository classTicketRepository;

    @BeforeEach
    void setUp() {
        classTicketRepository.deleteAll();
        fundingOrderRepository.deleteAll();
    }

    @Test
    @DisplayName("코드 등록 → 내 이용권 조회까지 한 흐름으로 동작한다")
    void register_thenGetMyTicket() throws Exception {
        String token = userToken();
        String code = newCode();
        issueTicket(code, RewardTier.SEAT_15, LocalDateTime.now().minusDays(3));

        mockMvc.perform(post("/api/v1/class-tickets/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", code.toLowerCase()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.capacity").value(15))
                .andExpect(jsonPath("$.data.expiresAt").exists());

        mockMvc.perform(get("/api/v1/class-tickets/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.capacity").value(15))
                .andExpect(jsonPath("$.data.remainingDays").isNumber());

        ClassTicket saved = classTicketRepository.findByCodeHash(TicketCodeHasher.hash(code)).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(TicketStatus.ACTIVE);
        assertThat(saved.getRegisteredUser()).isNotNull();
    }

    @Test
    @DisplayName("같은 코드를 다른 계정이 다시 등록하면 409 TICKET_409_CLAIMED")
    void register_rejectsAlreadyClaimedCode() throws Exception {
        String code = newCode();
        issueTicket(code, RewardTier.SEAT_5, LocalDateTime.now().minusDays(3));
        registerOk(userToken(), code);

        mockMvc.perform(post("/api/v1/class-tickets/register")
                        .header("Authorization", "Bearer " + userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", code))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("TICKET_409_CLAIMED"));
    }

    @Test
    @DisplayName("사용 중인 이용권이 있는 계정은 두 번째 코드를 등록할 수 없다")
    void register_rejectsSecondTicketForSameUser() throws Exception {
        String token = userToken();
        String first = newCode();
        String second = newCode();
        issueTicket(first, RewardTier.SEAT_5, LocalDateTime.now().minusDays(3));
        issueTicket(second, RewardTier.SEAT_30, LocalDateTime.now().minusDays(3));
        registerOk(token, first);

        mockMvc.perform(post("/api/v1/class-tickets/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", second))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("TICKET_409_ACTIVE"));
    }

    @Test
    @DisplayName("결제 후 1년이 지난 코드는 409 이고, 거절돼도 REGISTER_EXPIRED 상태는 DB 에 남는다")
    void register_registerWindowClosed_persistsStatus() throws Exception {
        String code = newCode();
        issueTicket(code, RewardTier.SEAT_5, LocalDateTime.now().minusYears(1).minusDays(1));

        mockMvc.perform(post("/api/v1/class-tickets/register")
                        .header("Authorization", "Bearer " + userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", code))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("TICKET_409_REG_EXP"));

        ClassTicket saved = classTicketRepository.findByCodeHash(TicketCodeHasher.hash(code)).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(TicketStatus.REGISTER_EXPIRED);
    }

    @Test
    @DisplayName("없는 코드는 404, 빈 코드는 400")
    void register_validation() throws Exception {
        String token = userToken();

        mockMvc.perform(post("/api/v1/class-tickets/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", "PICKE-NOPE-0000"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("TICKET_404_CODE"));

        mockMvc.perform(post("/api/v1/class-tickets/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", "  "))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("등록한 이용권이 없으면 404 TICKET_404, 토큰이 없으면 401")
    void getMyTicket_notFoundAndUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/class-tickets/me")
                        .header("Authorization", "Bearer " + userToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("TICKET_404"));

        mockMvc.perform(get("/api/v1/class-tickets/me"))
                .andExpect(status().isUnauthorized());
    }

    // ---- #535

    @Test
    @DisplayName("내가 등록한 코드를 다시 등록하면 200 으로 기존 이용권을 돌려주고 기간은 그대로다")
    void register_ownCodeAgain_returnsExistingResult() throws Exception {
        String token = userToken();
        String code = newCode();
        issueTicket(code, RewardTier.SEAT_5, LocalDateTime.now().minusDays(3));
        registerOk(token, code);
        ClassTicket first = classTicketRepository.findByCodeHash(TicketCodeHasher.hash(code)).orElseThrow();

        mockMvc.perform(post("/api/v1/class-tickets/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", code))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticketId").value(first.getId()))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        ClassTicket again = classTicketRepository.findByCodeHash(TicketCodeHasher.hash(code)).orElseThrow();
        assertThat(again.getActivatedAt()).isEqualTo(first.getActivatedAt());
        assertThat(again.getExpiresAt()).isEqualTo(first.getExpiresAt());
    }

    @Test
    @DisplayName("코드 미리보기는 정원·등록기한을 보여주고 코드를 소비하지 않는다. 등록 후엔 registeredByMe=true")
    void preview_doesNotConsumeCode() throws Exception {
        String token = userToken();
        String code = newCode();
        issueTicket(code, RewardTier.SEAT_15, LocalDateTime.now().minusDays(3));

        mockMvc.perform(get("/api/v1/class-tickets/preview")
                        .header("Authorization", "Bearer " + token)
                        .param("code", code.toLowerCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.capacity").value(15))
                .andExpect(jsonPath("$.data.registerBy").exists())
                .andExpect(jsonPath("$.data.usageMonths").value(6))
                .andExpect(jsonPath("$.data.registeredByMe").value(false));

        ClassTicket saved = classTicketRepository.findByCodeHash(TicketCodeHasher.hash(code)).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(TicketStatus.UNREGISTERED);
        assertThat(saved.getRegisteredUser()).isNull();

        registerOk(token, code);

        mockMvc.perform(get("/api/v1/class-tickets/preview")
                        .header("Authorization", "Bearer " + token)
                        .param("code", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.registeredByMe").value(true));
    }

    @Test
    @DisplayName("코드 미리보기 거절: 없는 코드 404, 빈 코드 400, 타인이 등록한 코드 409, 기한 지난 코드 409 (상태는 안 바뀜), 토큰 없음 401")
    void preview_rejections() throws Exception {
        String token = userToken();

        mockMvc.perform(get("/api/v1/class-tickets/preview")
                        .header("Authorization", "Bearer " + token)
                        .param("code", "PICKE-NOPE-0000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("TICKET_404_CODE"));

        mockMvc.perform(get("/api/v1/class-tickets/preview")
                        .header("Authorization", "Bearer " + token)
                        .param("code", " "))
                .andExpect(status().isBadRequest());

        String claimed = newCode();
        issueTicket(claimed, RewardTier.SEAT_5, LocalDateTime.now().minusDays(3));
        registerOk(userToken(), claimed);
        mockMvc.perform(get("/api/v1/class-tickets/preview")
                        .header("Authorization", "Bearer " + token)
                        .param("code", claimed))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("TICKET_409_CLAIMED"));

        String old = newCode();
        issueTicket(old, RewardTier.SEAT_5, LocalDateTime.now().minusYears(1).minusDays(1));
        mockMvc.perform(get("/api/v1/class-tickets/preview")
                        .header("Authorization", "Bearer " + token)
                        .param("code", old))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("TICKET_409_REG_EXP"));
        ClassTicket saved = classTicketRepository.findByCodeHash(TicketCodeHasher.hash(old)).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(TicketStatus.UNREGISTERED);

        mockMvc.perform(get("/api/v1/class-tickets/preview").param("code", old))
                .andExpect(status().isUnauthorized());
    }

    private void registerOk(String token, String code) throws Exception {
        mockMvc.perform(post("/api/v1/class-tickets/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", code))))
                .andExpect(status().isOk());
    }

    private static String newCode() {
        return "PICKE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void issueTicket(String code, RewardTier tier, LocalDateTime paidAt) {
        FundingOrder order = fundingOrderRepository.save(FundingOrder.builder()
                .platform(FundingPlatform.TUMBLBUG)
                .campaignId("camp-test")
                .orderId("order-" + UUID.randomUUID())
                .rewardTier(tier)
                .quantity(1)
                .supporterName("후원자")
                .paymentStatus(PaymentStatus.PAID_CONFIRMED)
                .paidAt(paidAt)
                .build());
        classTicketRepository.save(ClassTicket.issue(order, TicketCodeHasher.hash(code)));
    }

    private String userToken() {
        User user = userRepository.save(User.builder()
                .userTag("tk-" + UUID.randomUUID().toString().substring(0, 8))
                .nickname("tester")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
        return jwtProvider.createAccessToken(user.getId(), UserRole.USER.name());
    }
}

package com.swyp.picke.domain.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swyp.picke.domain.oauth.jwt.JwtProvider;
import com.swyp.picke.domain.ticket.entity.ClassTicket;
import com.swyp.picke.domain.ticket.entity.FundingOrder;
import com.swyp.picke.domain.ticket.enums.PaymentStatus;
import com.swyp.picke.domain.ticket.enums.TicketCodeStatus;
import com.swyp.picke.domain.ticket.enums.TicketStatus;
import com.swyp.picke.domain.ticket.repository.ClassTicketRepository;
import com.swyp.picke.domain.ticket.repository.FundingOrderRepository;
import com.swyp.picke.domain.ticket.repository.TicketDeliveryAttemptRepository;
import com.swyp.picke.domain.ticket.util.TicketCodeHasher;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.enums.UserRole;
import com.swyp.picke.domain.user.enums.UserStatus;
import com.swyp.picke.domain.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 어드민 운영 흐름을 HTTP 로 검증한다: 명단 업로드 → 발급 → 후원자 등록 / 전달 기록 → 재발급 → 환불.
 * 코드 원문은 발급 응답에서만 받고, DB 에는 해시만 있는지도 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminFundingApiIntegrationTest {

    private static final String HEADER =
            "platform,campaign_id,order_id,reward_tier,quantity,supporter_name,supporter_email,supporter_phone,payment_status,paid_at\n";
    private static final String CODE_PATTERN = "[0-9A-Z]{6}-[0-9A-Z]{5}-[0-9A-Z]{5}-[0-9A-Z]{5}-[0-9A-Z]{5}";

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
    @Autowired
    private TicketDeliveryAttemptRepository ticketDeliveryAttemptRepository;

    private String prefix;

    @BeforeEach
    void setUp() {
        prefix = "T" + UUID.randomUUID().toString().substring(0, 6).toUpperCase() + "-";
        cleanTickets();
    }

    @AfterEach
    void tearDown() {
        // 다른 테스트 클래스의 classTicketRepository.deleteAll() 이 FK 에 걸리지 않도록 전달 이력은 반드시 지운다
        cleanTickets();
    }

    private void cleanTickets() {
        ticketDeliveryAttemptRepository.deleteAll();
        classTicketRepository.deleteAll();
        fundingOrderRepository.deleteAll();
    }

    @Test
    @DisplayName("명단 업로드: 행 단위 오류는 errors 로 모으고, 발급 가능/보류 수를 돌려주며 재업로드는 멱등하다")
    void importCsv_countsAndIdempotent() throws Exception {
        String csv = HEADER
                + "TUMBLBUG,camp,ORD-A,SEAT_15,1,후원A,a@test.com,010-1,PAID_CONFIRMED,2026-10-01 10:00:00\n"
                + "TUMBLBUG,camp,ORD-B,SEAT_5,2,후원B,,,PAID_CONFIRMED,2026-10-01 10:00:00\n"
                + "TUMBLBUG,camp,ORD-C,5,1,후원C,,,PAYMENT_PENDING,\n"
                + "TUMBLBUG,camp,ORD-BAD,SEAT_99,1,후원D,,,PAID_CONFIRMED,2026-10-01 10:00:00\n";

        JsonNode first = upload(csv);
        assertThat(first.get("totalRows").asInt()).isEqualTo(4);
        assertThat(first.get("created").asInt()).isEqualTo(3);
        assertThat(first.get("updated").asInt()).isZero();
        assertThat(first.get("errorCount").asInt()).isEqualTo(1);
        assertThat(first.get("errors").get(0).get("line").asInt()).isEqualTo(5);
        assertThat(first.get("errors").get(0).get("orderId").asText()).isEqualTo(prefix + "ORD-BAD");
        assertThat(first.get("issuableCount").asLong()).isEqualTo(1);   // ORD-A 만
        assertThat(first.get("heldCount").asLong()).isEqualTo(2);       // ORD-B(수량 2), ORD-C(결제 대기)
        assertThat(first.get("fileHash").asText()).hasSize(64);

        // 같은 파일 재업로드 → 전부 unchanged, 결제 대기가 확정되면 updated
        JsonNode second = upload(csv);
        assertThat(second.get("created").asInt()).isZero();
        assertThat(second.get("unchanged").asInt()).isEqualTo(3);

        JsonNode third = upload(HEADER + "TUMBLBUG,camp,ORD-C,5,1,후원C,,,PAID_CONFIRMED,2026-10-05 10:00:00\n");
        assertThat(third.get("updated").asInt()).isEqualTo(1);
        assertThat(third.get("issuableCount").asLong()).isEqualTo(2);
        FundingOrder c = fundingOrderRepository.findByOrderId(prefix + "ORD-C").orElseThrow();
        assertThat(c.getPaymentStatus()).isEqualTo(PaymentStatus.PAID_CONFIRMED);
        assertThat(c.getImportFileHash()).isEqualTo(third.get("fileHash").asText());
    }

    @Test
    @DisplayName("발급 → 코드 1회 노출(DB 는 해시만) → 후원자가 그 코드로 등록 → 재실행은 중복 발급하지 않는다")
    void issue_thenUserRegisters_thenRerunSkips() throws Exception {
        upload(HEADER
                + "TUMBLBUG,camp,ORD-A,SEAT_15,1,후원A,a@test.com,,PAID_CONFIRMED,2026-10-01 10:00:00\n"
                + "TUMBLBUG,camp,ORD-B,SEAT_5,2,후원B,,,PAID_CONFIRMED,2026-10-01 10:00:00\n");

        JsonNode issued = postJson("/api/v1/admin/funding/tickets/issue", null, adminToken()).get("issued");
        assertThat(issued).hasSize(1);
        JsonNode item = issued.get(0);
        assertThat(item.get("orderId").asText()).isEqualTo(prefix + "ORD-A");
        assertThat(item.get("capacity").asInt()).isEqualTo(15);
        assertThat(item.get("registerBy").asText()).startsWith("2027-10-01T10:00");
        String code = item.get("code").asText();
        assertThat(code).matches(CODE_PATTERN);

        ClassTicket ticket = classTicketRepository.findById(item.get("ticketId").asLong()).orElseThrow();
        assertThat(ticket.getCodeHash()).isEqualTo(TicketCodeHasher.hash(code)).isNotEqualTo(code);
        assertThat(ticket.getCodeStatus()).isEqualTo(TicketCodeStatus.ISSUED);

        // 재실행: 살아 있는 이용권이 있으면 건너뛴다 (CF08)
        JsonNode rerun = postJson("/api/v1/admin/funding/tickets/issue", Map.of(), adminToken());
        assertThat(rerun.get("issued")).isEmpty();

        Long orderA = fundingOrderRepository.findByOrderId(prefix + "ORD-A").orElseThrow().getId();
        Long orderB = fundingOrderRepository.findByOrderId(prefix + "ORD-B").orElseThrow().getId();
        JsonNode byIds = postJson("/api/v1/admin/funding/tickets/issue",
                Map.of("fundingOrderIds", List.of(orderA, orderB, 999_999L)), adminToken());
        assertThat(byIds.get("issued")).isEmpty();
        assertThat(byIds.get("skipped")).hasSize(3);
        assertThat(byIds.get("skipped").get(0).get("reason").asText()).isEqualTo("ALREADY_ISSUED");
        assertThat(byIds.get("skipped").get(1).get("reason").asText()).isEqualTo("NOT_ISSUABLE");
        assertThat(byIds.get("skipped").get(2).get("reason").asText()).isEqualTo("NOT_FOUND");

        // 후원자가 받은 코드(소문자·하이픈 그대로)로 등록
        mockMvc.perform(post("/api/v1/class-tickets/register")
                        .header("Authorization", "Bearer " + userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", code.toLowerCase()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.capacity").value(15));

        // 등록된 이용권은 재발급 불가
        mockMvc.perform(post("/api/v1/admin/funding/tickets/" + ticket.getId() + "/reissue")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("TICKET_409_CLAIMED"));

        // 목록에는 등록자 정보가 나오고 코드·해시는 없다
        MvcResult list = mockMvc.perform(get("/api/v1/admin/funding/orders")
                        .param("codeStatus", "CLAIMED")
                        .param("keyword", prefix + "ORD-A")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].ticket.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.items[0].ticket.registeredUserId").isNumber())
                .andExpect(jsonPath("$.data.items[0].ticket.code").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].ticket.codeHash").doesNotExist())
                .andReturn();
        assertThat(list.getResponse().getContentAsString(StandardCharsets.UTF_8)).doesNotContain(code);
    }

    @Test
    @DisplayName("전달 기록 → 재발급하면 옛 코드는 REVOKED, 새 코드는 같은 등록기한으로 등록된다")
    void delivery_thenReissue() throws Exception {
        upload(HEADER + "WADIZ,camp,ORD-R,SEAT_30,1,후원R,r@test.com,,PAID_CONFIRMED,2026-09-15 09:00:00\n");
        JsonNode first = postJson("/api/v1/admin/funding/tickets/issue", null, adminToken()).get("issued").get(0);
        long ticketId = first.get("ticketId").asLong();
        String oldCode = first.get("code").asText();

        JsonNode delivery = postJson("/api/v1/admin/funding/tickets/" + ticketId + "/deliveries",
                Map.of("channel", "EMAIL", "status", "SUCCESS"), adminToken());
        assertThat(delivery.get("codeStatus").asText()).isEqualTo("DELIVERED");
        assertThat(delivery.get("deliveredAt").isNull()).isFalse();
        assertThat(ticketDeliveryAttemptRepository.findByTicketIdOrderByAttemptedAtDesc(ticketId)).hasSize(1);

        JsonNode reissued = postJson("/api/v1/admin/funding/tickets/" + ticketId + "/reissue", null, adminToken());
        String newCode = reissued.get("code").asText();
        assertThat(newCode).matches(CODE_PATTERN).isNotEqualTo(oldCode);
        assertThat(reissued.get("registerBy").asText()).isEqualTo(first.get("registerBy").asText());
        assertThat(reissued.get("capacity").asInt()).isEqualTo(30);

        ClassTicket old = classTicketRepository.findById(ticketId).orElseThrow();
        assertThat(old.getStatus()).isEqualTo(TicketStatus.REVOKED);
        assertThat(old.getRevokedAt()).isNotNull();
        assertThat(classTicketRepository.findByFundingOrderIdAndStatusNot(old.getFundingOrder().getId(), TicketStatus.REVOKED))
                .get().extracting(ClassTicket::getId).isEqualTo(reissued.get("ticketId").asLong());

        // 옛 코드는 확인·등록 모두 409, 재발급·전달 기록도 거절
        mockMvc.perform(get("/api/v1/class-tickets/preview").param("code", oldCode)
                        .header("Authorization", "Bearer " + userToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("TICKET_409_REVOKED"));
        mockMvc.perform(post("/api/v1/admin/funding/tickets/" + ticketId + "/reissue")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("TICKET_409_REVOKED"));
        mockMvc.perform(post("/api/v1/admin/funding/tickets/" + ticketId + "/deliveries")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("channel", "SMS", "status", "SUCCESS"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("TICKET_409_STATE"));

        // 새 코드는 정상 등록
        mockMvc.perform(post("/api/v1/class-tickets/register")
                        .header("Authorization", "Bearer " + userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", newCode))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.capacity").value(30));
    }

    @Test
    @DisplayName("환불하면 주문 REFUNDED + 이용권 REVOKED 이고, 결제 확정 명단을 다시 올려도 되살아나지 않는다")
    void refund_isTerminal() throws Exception {
        String csv = HEADER
                + "TUMBLBUG,camp,ORD-F,SEAT_5,1,후원F,,,PAID_CONFIRMED,2026-10-01 10:00:00\n"
                + "TUMBLBUG,camp,ORD-G,SEAT_5,1,후원G,,,PAID_CONFIRMED,2026-10-01 10:00:00\n";
        upload(csv);
        postJson("/api/v1/admin/funding/tickets/issue", null, adminToken());
        Long orderF = fundingOrderRepository.findByOrderId(prefix + "ORD-F").orElseThrow().getId();

        JsonNode refund = postJson("/api/v1/admin/funding/refunds", Map.of("fundingOrderIds", List.of(orderF)), adminToken());
        assertThat(refund.get("refunded").asInt()).isEqualTo(1);
        assertThat(refund.get("revokedTickets").asInt()).isEqualTo(1);

        FundingOrder f = fundingOrderRepository.findById(orderF).orElseThrow();
        assertThat(f.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(f.getPaidAt()).isNotNull();
        assertThat(classTicketRepository.findByFundingOrderIdAndStatusNot(orderF, TicketStatus.REVOKED)).isEmpty();

        JsonNode again = postJson("/api/v1/admin/funding/refunds", Map.of("fundingOrderIds", List.of(orderF)), adminToken());
        assertThat(again.get("refunded").asInt()).isZero();
        assertThat(again.get("alreadyRefunded").get(0).asLong()).isEqualTo(orderF);

        JsonNode reupload = upload(csv);
        assertThat(reupload.get("errorCount").asInt()).isEqualTo(1);
        assertThat(reupload.get("errors").get(0).get("orderId").asText()).isEqualTo(prefix + "ORD-F");
        assertThat(reupload.get("errors").get(0).get("reason").asText()).contains("REFUNDED");
        assertThat(fundingOrderRepository.findById(orderF).orElseThrow().getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);

        mockMvc.perform(post("/api/v1/admin/funding/refunds")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("fundingOrderIds", List.of(orderF, 999_999L)))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("FUNDING_404_ORDER"));

        mockMvc.perform(get("/api/v1/admin/funding/summary")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalOrders").value(2))
                .andExpect(jsonPath("$.data.paidConfirmed").value(1))
                .andExpect(jsonPath("$.data.issued").value(1))
                .andExpect(jsonPath("$.data.undelivered").value(1))
                .andExpect(jsonPath("$.data.refunded").value(1));
    }

    @Test
    @DisplayName("일반 유저는 어드민 펀딩 API 에 접근할 수 없고, 헤더가 다른 CSV 는 400 FUNDING_400_CSV")
    void forbiddenAndInvalidCsv() throws Exception {
        mockMvc.perform(get("/api/v1/admin/funding/summary")
                        .header("Authorization", "Bearer " + userToken()))
                .andExpect(status().isForbidden());

        mockMvc.perform(multipart("/api/v1/admin/funding/imports")
                        .file(new MockMultipartFile("file", "bad.csv", "text/csv",
                                "order_id,name\nORD-1,후원\n".getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("FUNDING_400_CSV"));
    }

    // ───────────── helpers ─────────────

    /** order_id 에 테스트별 prefix 를 붙여 같은 DB 를 쓰는 다른 테스트와 겹치지 않게 한다 */
    private JsonNode upload(String csv) throws Exception {
        String prefixed = csv.replace(",ORD-", "," + prefix + "ORD-");
        MvcResult result = mockMvc.perform(multipart("/api/v1/admin/funding/imports")
                        .file(new MockMultipartFile("file", "orders.csv", "text/csv",
                                prefixed.getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andReturn();
        return data(result);
    }

    private JsonNode postJson(String url, Object body, String token) throws Exception {
        var request = post(url).header("Authorization", "Bearer " + token);
        if (body != null) {
            request = request.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
        }
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        return data(result);
    }

    private JsonNode data(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).get("data");
    }

    private String adminToken() {
        User admin = userRepository.save(User.builder()
                .userTag("adm-" + UUID.randomUUID().toString().substring(0, 8))
                .nickname("adm")
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .build());
        return jwtProvider.createAccessToken(admin.getId(), admin.getRole().name());
    }

    private String userToken() {
        User user = userRepository.save(User.builder()
                .userTag("usr-" + UUID.randomUUID().toString().substring(0, 8))
                .nickname("후원자")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
        return jwtProvider.createAccessToken(user.getId(), user.getRole().name());
    }
}

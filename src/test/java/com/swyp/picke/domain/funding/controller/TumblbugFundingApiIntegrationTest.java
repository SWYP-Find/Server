package com.swyp.picke.domain.funding.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.enums.BattleCreatorType;
import com.swyp.picke.domain.battle.enums.BattleStatus;
import com.swyp.picke.domain.battle.repository.BattleRepository;
import com.swyp.picke.domain.classroom.repository.ClassroomRepository;
import com.swyp.picke.domain.classroom.service.ClassAccessService;
import com.swyp.picke.domain.funding.repository.TumblbugImportRecordRepository;
import com.swyp.picke.domain.oauth.jwt.JwtProvider;
import com.swyp.picke.domain.ticket.enums.TicketStatus;
import com.swyp.picke.domain.ticket.repository.ClassTicketRepository;
import com.swyp.picke.domain.ticket.repository.FundingOrderRepository;
import com.swyp.picke.domain.ticket.util.TicketCodeHasher;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.enums.UserRole;
import com.swyp.picke.domain.user.enums.UserStatus;
import com.swyp.picke.domain.user.repository.UserRepository;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/** Verifies the XLSX/API boundary against the actual canonical ticket and classroom flows. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TumblbugFundingApiIntegrationTest {
    private static final String BASE = "/api/v1/admin/funding-orders/tumblbug";
    private static final String SHEET = "15인 이용권";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JwtProvider jwtProvider;
    @Autowired UserRepository users;
    @Autowired FundingOrderRepository orders;
    @Autowired ClassTicketRepository tickets;
    @Autowired TumblbugImportRecordRepository imports;
    @Autowired BattleRepository battles;
    @Autowired ClassroomRepository classrooms;
    @Autowired ClassAccessService classAccess;
    private String campaign;
    private String adminToken;
    private String userToken;
    private User user;

    @BeforeEach
    void setUp() {
        campaign = "tb-" + UUID.randomUUID();
        adminToken = token(newUser(UserRole.ADMIN));
        user = newUser(UserRole.USER);
        userToken = token(user);
    }

    @Test
    void xlsxTicketCanCreateClassAndRefundEndsAccess() throws Exception {
        byte[] source = workbook(row("101", "클래스 19900원", "결제완료"));
        long before = orders.count();
        mvc.perform(multipart(BASE + "/inspect").file(file(source)).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.sheets[0].sheetName").value(SHEET));
        mvc.perform(upload("/preview", source)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.codeEligibleRows").value(1));
        assertThat(orders.count()).isEqualTo(before);

        String code = issue(source);
        var ticket = tickets.findByCodeHash(TicketCodeHasher.hash(code)).orElseThrow();
        long ticketId = ticket.getId();
        long orderId = ticket.getFundingOrder().getId();
        assertThat(ticket.getCodeSuffix()).isEqualTo(code.substring(code.length() - 4));
        assertThat(ticket.getCodeHash()).doesNotContain(code);

        mvc.perform(get("/api/v1/admin/funding/orders").param("keyword", "101")
                        .param("capacity", "15").param("deliveryStatus", "PENDING")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].fundingOrderId").value(orderId))
                .andExpect(jsonPath("$.data.items[0].tumblbug.rewardAmount").value(19900));
        delivery("101", "SENT", null).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updatedAttemptCount").value(1));
        delivery("101", "SENT", null).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updatedAttemptCount").value(0));

        mvc.perform(get("/api/v1/class-tickets/preview").param("code", code)
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.capacity").value(15));
        jsonPost("/api/v1/class-tickets/register", Map.of("code", code), userToken)
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.ticketId").value(ticketId));
        jsonPost("/api/v1/class-tickets/register", Map.of("code", code), userToken)
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.ticketId").value(ticketId));
        Battle battle = battles.save(Battle.builder().title("텀블벅 클래스 검증").summary("요약").description("설명")
                .thumbnailUrl("https://example.com/test.png").targetDate(LocalDate.now())
                .status(BattleStatus.PUBLISHED).creatorType(BattleCreatorType.ADMIN).build());
        JsonNode created = data(jsonPost("/api/v1/classes", Map.of("name", "후원 클래스", "battleId", battle.getId(),
                "commentRequired", false, "ownerParticipates", true), userToken).andExpect(status().isOk()).andReturn());
        long classId = created.get("classId").asLong();
        assertThat(classrooms.findById(classId).orElseThrow().getTicket().getId()).isEqualTo(ticketId);
        jsonPost("/api/v1/admin/funding/refunds", Map.of("fundingOrderIds", List.of(orderId)), adminToken)
                .andExpect(status().isOk());
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.REVOKED);
        assertThatThrownBy(() -> classAccess.requireWritable(classId, user.getId(), LocalDateTime.now(ZoneId.of("Asia/Seoul"))))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CLASS_TICKET_ENDED));
    }

    @Test
    void duplicateImportIsRejectedAndHeldRowsDoNotIssueTickets() throws Exception {
        byte[] source = workbook(row("201", "클래스 19900원", "결제완료"),
                row("202", "클래스 19900원 (x2)", "결제완료"), row("203", "클래스 19900원", "결제대기"));
        mvc.perform(upload("/preview", source)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.importableRows").value(3)).andExpect(jsonPath("$.data.codeEligibleRows").value(1));
        issue(source);
        long orderCount = orders.count();
        long ticketCount = tickets.count();
        assertThat(imports.findAll().stream().filter(r -> r.getCampaign().getExternalCampaignId().equals(campaign))).hasSize(3);
        mvc.perform(upload("/digital-gifts", source)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("FUNDING_409_TUMBLBUG_DUPLICATE"));
        assertThat(orders.count()).isEqualTo(orderCount);
        assertThat(tickets.count()).isEqualTo(ticketCount);
    }

    @Test
    void invalidRowPreventsPartialImport() throws Exception {
        byte[] source = workbook(row("301", "클래스 19900원", "결제완료"), row("302", "금액없는선물", "결제완료"));
        long before = orders.count();
        mvc.perform(upload("/digital-gifts", source)).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("FUNDING_422_TUMBLBUG_ROWS"));
        assertThat(orders.count()).isEqualTo(before);
    }

    @Test
    void latestDeliveryFiltersAndReissuedCodeSearchUseCanonicalTickets() throws Exception {
        String code = issue(workbook(row("401", "클래스 19900원", "결제완료")));
        var ticket = tickets.findByCodeHash(TicketCodeHasher.hash(code)).orElseThrow();
        delivery("401", "FAILED", "업로드 실패").andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/funding/orders").param("keyword", "401").param("deliveryStatus", "FAILED")
                        .header("Authorization", bearer(adminToken))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].deliveryStatus").value("FAILED"));
        mvc.perform(get("/api/v1/admin/funding/orders").param("keyword", "401").param("deliveryStatus", "SENT")
                        .header("Authorization", bearer(adminToken))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty());
        JsonNode reissued = data(jsonPost("/api/v1/admin/funding/tickets/" + ticket.getId() + "/reissue", null, adminToken)
                .andExpect(status().isOk()).andReturn());
        String newCode = reissued.get("code").asText();
        String suffix = newCode.substring(newCode.length() - 4);
        mvc.perform(get("/api/v1/admin/tickets").param("keyword", suffix).param("capacity", "15")
                        .header("Authorization", bearer(adminToken))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].ticketId").value(reissued.get("ticketId").asLong()))
                .andExpect(jsonPath("$.data.content[0].maskedCode").value("****-" + suffix));
        delivery("401", "SENT", null).andExpect(status().isOk()).andExpect(jsonPath("$.data.updatedAttemptCount").value(1));
        mvc.perform(get("/api/v1/admin/funding/orders").param("keyword", "401").param("deliveryStatus", "SENT")
                        .header("Authorization", bearer(adminToken))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].deliveryStatus").value("SENT"));
    }

    @Test
    void adminRoleAndDeliveryValidationAreEnforced() throws Exception {
        byte[] source = workbook(row("501", "클래스 19900원", "결제완료"));
        mvc.perform(multipart(BASE + "/inspect").file(file(source)).header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden());
        issue(source);
        delivery("501", "FAILED", null).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/admin/tickets").param("registeredFrom", "2026-10-21")
                        .param("registeredTo", "2026-10-20").header("Authorization", bearer(adminToken)))
                .andExpect(status().isBadRequest());
    }

    private String issue(byte[] source) throws Exception {
        byte[] result = mvc.perform(upload("/digital-gifts", source)).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store, no-cache, must-revalidate"))
                .andReturn().getResponse().getContentAsByteArray();
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            assertThat(workbook.getSheetAt(0).getLastRowNum()).isEqualTo(1);
            return workbook.getSheetAt(0).getRow(1).getCell(1).getStringCellValue();
        }
    }

    private MockMultipartHttpServletRequestBuilder upload(String endpoint, byte[] source) throws Exception {
        var request = multipart(BASE + endpoint).file(file(source)).file(new MockMultipartFile("options", "options.json",
                "application/json", mapper.writeValueAsBytes(Map.of("campaignId", campaign, "campaignName", "테스트 캠페인",
                        "sheetName", SHEET, "rewardLineId", "seat15", "capacity", 15))));
        request.header("Authorization", bearer(adminToken));
        return request;
    }

    private org.springframework.test.web.servlet.ResultActions delivery(String number, String result, String reason) throws Exception {
        var body = new java.util.HashMap<String, Object>();
        body.put("campaignId", campaign); body.put("rewardLineId", "seat15");
        body.put("supporterNumbers", List.of(number)); body.put("status", result);
        if (reason != null) body.put("failedReason", reason);
        return mvc.perform(patch(BASE + "/digital-gifts/delivery").header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsBytes(body)));
    }

    private org.springframework.test.web.servlet.ResultActions jsonPost(String url, Object body, String token) throws Exception {
        var request = post(url).header("Authorization", bearer(token));
        if (body != null) request.contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsBytes(body));
        return mvc.perform(request);
    }

    private JsonNode data(MvcResult result) throws Exception {
        return mapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).get("data");
    }

    private String[] row(String number, String gift, String status) {
        String paidAt = LocalDateTime.now(ZoneId.of("Asia/Seoul")).minusDays(10)
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return new String[]{number, "후원자", gift, "0", "19900", paidAt, "신청", status, "미전달"};
    }

    private byte[] workbook(String[]... rows) throws Exception {
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet(SHEET);
            String[] header = {"후원번호", "이름", "선물", "추가 후원금", "총 후원 금액", "후원 일시", "알림 신청", "결제 상태", "선물 전달"};
            for (int i = -1; i < rows.length; i++) {
                var row = sheet.createRow(i + 1);
                String[] values = i < 0 ? header : rows[i];
                for (int c = 0; c < values.length; c++) row.createCell(c).setCellValue(values[c]);
            }
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private MockMultipartFile file(byte[] bytes) {
        return new MockMultipartFile("file", "supporters.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);
    }

    private User newUser(UserRole role) {
        return users.save(User.builder().userTag("tb-" + UUID.randomUUID().toString().substring(0, 8))
                .nickname("후원자").role(role).status(UserStatus.ACTIVE).build());
    }
    private String token(User user) { return jwtProvider.createAccessToken(user.getId(), user.getRole().name()); }
    private String bearer(String token) { return "Bearer " + token; }
}

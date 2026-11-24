package com.swyp.picke.domain.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swyp.picke.domain.admin.dto.funding.request.FundingRefundRequest;
import com.swyp.picke.domain.admin.dto.funding.request.FundingOrderContactUpdateRequest;
import com.swyp.picke.domain.admin.dto.funding.request.FundingOrderAdminUpdateRequest;
import com.swyp.picke.domain.admin.dto.funding.request.TicketDeliveryRequest;
import com.swyp.picke.domain.admin.dto.funding.request.RegisterReminderRequest;
import com.swyp.picke.domain.admin.dto.funding.request.TicketIssueRequest;
import com.swyp.picke.domain.admin.dto.funding.request.TicketExpiresAtRequest;
import com.swyp.picke.domain.admin.dto.funding.response.TicketExpiresAtResponse;
import com.swyp.picke.domain.admin.dto.funding.request.TicketUsagePeriodRequest;
import com.swyp.picke.domain.admin.dto.funding.response.FundingImportResponse;
import com.swyp.picke.domain.admin.dto.funding.response.FundingOrderPageResponse;
import com.swyp.picke.domain.admin.dto.funding.response.FundingOrderSummaryResponse;
import com.swyp.picke.domain.admin.dto.funding.response.FundingRefundResponse;
import com.swyp.picke.domain.admin.dto.funding.response.FundingSummaryResponse;
import com.swyp.picke.domain.admin.dto.funding.response.RegisterReminderResponse;
import com.swyp.picke.domain.admin.dto.funding.response.RegisterReminderTargetResponse;
import com.swyp.picke.domain.admin.dto.funding.response.TumblbugCampaignDashboardResponse;
import com.swyp.picke.domain.admin.dto.funding.response.TicketDeliveryResponse;
import com.swyp.picke.domain.admin.dto.funding.response.TicketIssueResponse;
import com.swyp.picke.domain.admin.dto.funding.response.TicketUsagePeriodResponse;
import com.swyp.picke.domain.admin.dto.funding.response.AdminTicketCodeResponse;
import com.swyp.picke.domain.ticket.entity.ClassTicket;
import com.swyp.picke.domain.ticket.entity.FundingOrder;
import com.swyp.picke.domain.ticket.entity.TicketDeliveryAttempt;
import com.swyp.picke.domain.ticket.entity.TicketRegisterReminder;
import com.swyp.picke.domain.ticket.enums.DeliveryStatus;
import com.swyp.picke.domain.ticket.enums.DeliveryChannel;
import com.swyp.picke.domain.ticket.enums.PaymentStatus;
import com.swyp.picke.domain.ticket.enums.RegisterReminderStage;
import com.swyp.picke.domain.ticket.enums.FundingPlatform;
import com.swyp.picke.domain.ticket.enums.RewardTier;
import com.swyp.picke.domain.funding.dto.response.TumblbugOrderDetailResponse;
import com.swyp.picke.domain.funding.entity.AdminAuditLog;
import com.swyp.picke.domain.funding.repository.AdminAuditLogRepository;
import com.swyp.picke.domain.funding.service.TumblbugImportRecordService;
import com.swyp.picke.domain.ticket.enums.TicketCodeStatus;
import com.swyp.picke.domain.ticket.enums.TicketStatus;
import com.swyp.picke.domain.ticket.repository.ClassTicketRepository;
import com.swyp.picke.domain.ticket.repository.FundingOrderRepository;
import com.swyp.picke.domain.ticket.repository.TicketDeliveryAttemptRepository;
import com.swyp.picke.domain.ticket.repository.TicketRegisterReminderRepository;
import com.swyp.picke.domain.classroom.repository.ClassroomRepository;
import com.swyp.picke.domain.ticket.util.FundingCsvParser;
import com.swyp.picke.domain.ticket.util.TicketCodeGenerator;
import com.swyp.picke.domain.ticket.util.TicketCodeHasher;
import com.swyp.picke.domain.ticket.service.TicketCodeVault;
import com.swyp.picke.domain.funding.service.TumblbugDigitalGiftWorkbookWriter;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 어드민 펀딩 명단·이용권 운영 (PRD §30~§33).
 * <ul>
 *   <li>명단 CSV 업로드: order_id 기준 upsert (CF01). 환불·취소는 종착 상태라 되살리지 않는다 (CF07).</li>
 *   <li>발급: 결제 확정·미발급 수량만큼 코드 생성. SHA-256 해시와 AES-GCM 암호문을 저장합니다.</li>
 *   <li>재발급: 기존 행 REVOKED + 같은 등록기한의 새 행 (CF06). 환불: 주문 REFUNDED + 살아 있는 이용권 REVOKED.</li>
 * </ul>
 * 코드 원문은 어떤 경로로도 로그에 남기지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminFundingService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int CODE_RETRY_LIMIT = 5;

    private final FundingOrderRepository fundingOrderRepository;
    private final ClassroomRepository classroomRepository;
    private final ClassTicketRepository classTicketRepository;
    private final TicketDeliveryAttemptRepository ticketDeliveryAttemptRepository;
    private final TicketRegisterReminderRepository ticketRegisterReminderRepository;
    private final TumblbugImportRecordService tumblbugImportRecordService;
    private final TicketCodeVault ticketCodeVault;
    private final TumblbugDigitalGiftWorkbookWriter tumblbugWorkbookWriter;
    private final AdminAuditLogRepository adminAuditLogRepository;
    private final ObjectMapper objectMapper;

    // ───────────── 1. 명단 가져오기 ─────────────

    @Transactional
    public FundingImportResponse importCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.FILE_NOT_FOUND);
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new CustomException(ErrorCode.FUNDING_CSV_INVALID);
        }
        String fileHash = sha256Hex(bytes);
        FundingCsvParser.Result parsed = FundingCsvParser.parse(new ByteArrayInputStream(bytes));

        List<FundingImportResponse.RowError> errors = new ArrayList<>();
        parsed.errors().forEach(e -> errors.add(new FundingImportResponse.RowError(e.line(), e.orderId(), e.reason())));

        List<String> orderIds = parsed.rows().stream().map(FundingCsvParser.Row::orderId).toList();
        Map<String, FundingOrder> existingByOrderId = fundingOrderRepository.findByOrderIdIn(orderIds).stream()
                .collect(Collectors.toMap(FundingOrder::getOrderId, Function.identity()));
        Map<Long, Long> liveTicketCountByOrder = classTicketRepository
                .findByFundingOrderIdInAndStatusNot(
                        existingByOrderId.values().stream().map(FundingOrder::getId).toList(), TicketStatus.REVOKED)
                .stream().collect(Collectors.groupingBy(t -> t.getFundingOrder().getId(), Collectors.counting()));

        int created = 0;
        int updated = 0;
        int unchanged = 0;
        for (FundingCsvParser.Row row : parsed.rows()) {
            FundingOrder existing = existingByOrderId.get(row.orderId());
            if (existing == null) {
                fundingOrderRepository.save(FundingOrder.builder()
                        .platform(row.platform())
                        .campaignId(row.campaignId())
                        .orderId(row.orderId())
                        .rewardTier(row.rewardTier())
                        .quantity(row.quantity())
                        .supporterName(row.supporterName())
                        .supporterEmail(row.supporterEmail())
                        .supporterPhone(row.supporterPhone())
                        .paymentStatus(row.paymentStatus())
                        .paidAt(row.paidAt())
                        .importFileHash(fileHash)
                        .build());
                created++;
                continue;
            }
            if (existing.isTerminal() && !isTerminal(row.paymentStatus())) {
                errors.add(new FundingImportResponse.RowError(row.line(), row.orderId(),
                        "이미 " + existing.getPaymentStatus() + " 처리된 주문은 " + row.paymentStatus() + " 로 되돌릴 수 없습니다."));
                continue;
            }
            long liveTicketCount = liveTicketCountByOrder.getOrDefault(existing.getId(), 0L);
            if (liveTicketCount > 0 && existing.getRewardTier() != row.rewardTier()) {
                errors.add(new FundingImportResponse.RowError(row.line(), row.orderId(),
                        "이용권이 이미 발급된 주문의 reward_tier 는 변경할 수 없습니다. 환불 처리 후 다시 올려 주세요."));
                continue;
            }
            if (liveTicketCount > row.quantity()) {
                errors.add(new FundingImportResponse.RowError(row.line(), row.orderId(),
                        "이미 발급된 코드 수보다 quantity 를 작게 변경할 수 없습니다."));
                continue;
            }
            boolean changed = existing.applyImport(row.platform(), row.campaignId(), row.rewardTier(), row.quantity(),
                    row.supporterName(), row.supporterEmail(), row.supporterPhone(),
                    row.paymentStatus(), row.paidAt(), fileHash);
            if (changed) {
                updated++;
            } else {
                unchanged++;
            }
        }
        fundingOrderRepository.flush();

        return new FundingImportResponse(fileHash, parsed.totalRows(), created, updated, unchanged, errors.size(),
                fundingOrderRepository.countIssuable(), fundingOrderRepository.countHeld(), errors);
    }

    private static boolean isTerminal(PaymentStatus status) {
        return status == PaymentStatus.REFUNDED || status == PaymentStatus.CANCELED;
    }

    // ───────────── 2. 발급 ─────────────

    @Transactional
    public TicketIssueResponse issueTickets(TicketIssueRequest request) {
        List<TicketIssueResponse.IssuedTicket> issued = new ArrayList<>();
        List<TicketIssueResponse.Skipped> skipped = new ArrayList<>();

        if (request == null || request.fundingOrderIds() == null || request.fundingOrderIds().isEmpty()) {
            throw new CustomException(ErrorCode.FUNDING_INVALID_REQUEST);
        }
        List<Long> targetIds = new ArrayList<>();
        List<Long> ids = request.fundingOrderIds().stream().distinct().sorted().toList();
        Map<Long, FundingOrder> found = fundingOrderRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(FundingOrder::getId, Function.identity()));
        for (Long id : ids) {
            FundingOrder order = found.get(id);
            if (order == null) {
                skipped.add(new TicketIssueResponse.Skipped(id, null, "NOT_FOUND"));
            } else if (!order.isIssuable() || order.getPaidAt() == null) {
                skipped.add(new TicketIssueResponse.Skipped(id, order.getOrderId(), "NOT_ISSUABLE"));
            } else {
                targetIds.add(id);
            }
        }

        if (targetIds.isEmpty()) return new TicketIssueResponse(issued, skipped);
        targetIds.sort(Long::compareTo);
        List<FundingOrder> lockedTargets = fundingOrderRepository.findAllByIdForUpdate(targetIds);
        Map<Long, Long> liveCountByOrder = classTicketRepository.findByFundingOrderIdInAndStatusNot(
                        lockedTargets.stream().map(FundingOrder::getId).toList(), TicketStatus.REVOKED)
                .stream().collect(Collectors.groupingBy(ticket -> ticket.getFundingOrder().getId(), Collectors.counting()));
        for (FundingOrder order : lockedTargets) {
            if (!order.isIssuable()) {
                skipped.add(new TicketIssueResponse.Skipped(order.getId(), order.getOrderId(), "NOT_ISSUABLE"));
                continue;
            }
            long liveCount = liveCountByOrder.getOrDefault(order.getId(), 0L);
            if (liveCount >= order.getQuantity()) {
                skipped.add(new TicketIssueResponse.Skipped(order.getId(), order.getOrderId(), "ALREADY_ISSUED"));
                continue;
            }
            for (long i = liveCount; i < order.getQuantity(); i++) {
                String rawCode = generateUniqueCode();
                ClassTicket ticket = classTicketRepository.save(ClassTicket.issue(order, TicketCodeHasher.hash(rawCode),
                        ticketCodeVault.encrypt(TicketCodeGenerator.format(rawCode))));
                ticket.recordCodeSuffix(rawCode);
                issued.add(toIssued(ticket, rawCode));
            }
        }
        return new TicketIssueResponse(issued, skipped);
    }

    @Transactional
    public byte[] createIssuedTicketWorkbook(List<Long> ticketIds, Long adminUserId) {
        List<Long> ids = ticketIds == null ? List.of() : ticketIds.stream().distinct().sorted().toList();
        if (ids.isEmpty()) throw new CustomException(ErrorCode.FUNDING_TUMBLBUG_IMPORT_INVALID);
        Map<Long, ClassTicket> tickets = classTicketRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(ClassTicket::getId, Function.identity()));
        if (tickets.size() != ids.size()) throw new CustomException(ErrorCode.TICKET_NOT_FOUND);
        List<Long> orderIds = tickets.values().stream().map(ticket -> ticket.getFundingOrder().getId()).distinct().toList();
        Map<Long, TumblbugOrderDetailResponse> details = tumblbugImportRecordService.getDetails(orderIds);
        Map<String, String> codesBySupporter = new java.util.TreeMap<>();
        Set<String> campaignIds = new java.util.HashSet<>();
        for (Long ticketId : ids) {
            ClassTicket ticket = tickets.get(ticketId);
            if (ticket.getFundingOrder().getPlatform() != FundingPlatform.TUMBLBUG
                    || ticket.getStatus() != TicketStatus.UNREGISTERED
                    || ticket.getCodeStatus() == TicketCodeStatus.REVOKED) {
                throw new CustomException(ErrorCode.FUNDING_TUMBLBUG_IMPORT_INVALID);
            }
            TumblbugOrderDetailResponse detail = details.get(ticket.getFundingOrder().getId());
            if (detail == null || detail.supporterNumber() == null || detail.supporterNumber().isBlank()) {
                throw new CustomException(ErrorCode.FUNDING_TUMBLBUG_IMPORT_INVALID);
            }
            campaignIds.add(ticket.getFundingOrder().getCampaignId());
            if (campaignIds.size() > 1) throw new CustomException(ErrorCode.FUNDING_TUMBLBUG_IMPORT_INVALID);
            String code = getTicketCode(ticketId, adminUserId).code();
            codesBySupporter.merge(detail.supporterNumber(), code, (current, next) -> current + "\n" + next);
        }
        return tumblbugWorkbookWriter.write(codesBySupporter);
    }

    /** 해시 충돌(사실상 0에 가깝지만)은 재생성으로 흡수. 원문은 반환값으로만 흐른다 */
    private String generateUniqueCode() {
        for (int i = 0; i < CODE_RETRY_LIMIT; i++) {
            String raw = TicketCodeGenerator.generate();
            if (!classTicketRepository.existsByCodeHash(TicketCodeHasher.hash(raw))) {
                return raw;
            }
        }
        throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    private static TicketIssueResponse.IssuedTicket toIssued(ClassTicket ticket, String rawCode) {
        FundingOrder order = ticket.getFundingOrder();
        return new TicketIssueResponse.IssuedTicket(ticket.getId(), order.getId(), order.getOrderId(),
                order.getSupporterName(), order.getSupporterEmail(), order.getSupporterPhone(),
                ticket.getCapacity(), ticket.getRegisterBy(), TicketCodeGenerator.format(rawCode));
    }

    // ───────────── 2-1. 사용 기간 ─────────────

    /**
     * 등록 전 이용권의 사용 기간(일)을 한 번에 정한다 (#685). 하나라도 없거나 이미 등록·무효화된 이용권이면 전부 반영하지 않는다.
     * 등록 시 만료일 = 등록 시각 + usageDays 일. null 이면 기본 6개월.
     */
    @Transactional
    public TicketUsagePeriodResponse updateUsagePeriod(TicketUsagePeriodRequest request, Long adminUserId) {
        List<Long> ids = request.ticketIds().stream().distinct().sorted().toList();
        List<ClassTicket> tickets = classTicketRepository.findAllById(ids);
        if (tickets.size() != ids.size()) {
            throw new CustomException(ErrorCode.TICKET_NOT_FOUND);
        }
        if (tickets.stream().anyMatch(ticket -> !ticket.isUnclaimed() || ticket.isRevoked())) {
            throw new CustomException(ErrorCode.TICKET_STATE_CONFLICT);
        }
        String metadata = "{\"usageDays\":" + request.usageDays() + "}";
        for (ClassTicket ticket : tickets) {
            ticket.updateUsageDays(request.usageDays());
            adminAuditLogRepository.save(new AdminAuditLog(adminUserId, "TICKET_USAGE_PERIOD_UPDATED", "CLASS_TICKET",
                    ticket.getId(), null, metadata));
        }
        return new TicketUsagePeriodResponse(ids, request.usageDays());
    }

    /**
     * 등록된 이용권의 만료일을 바꾼다 (#691). 클래스 마감·결과 조회 기간은 이용권 만료일을 따라가므로 함께 바뀐다.
     * <ul>
     *   <li>등록 전·무효화된 이용권은 409 TICKET_409_STATE (등록 전은 사용 기간 설정 API 사용)</li>
     *   <li>등록 시각 이전으로는 바꿀 수 없다 (400)</li>
     *   <li>만료된 이용권을 미래로 늘릴 때 그 계정에 다른 사용 중 이용권이 있으면 409 TICKET_409_ACTIVE</li>
     * </ul>
     */
    @Transactional
    public TicketExpiresAtResponse changeExpiresAt(Long ticketId, TicketExpiresAtRequest request, Long adminUserId) {
        ClassTicket ticket = getTicket(ticketId);
        if (ticket.isRevoked() || ticket.getActivatedAt() == null
                || (ticket.getStatus() != TicketStatus.ACTIVE && ticket.getStatus() != TicketStatus.EXPIRED)) {
            throw new CustomException(ErrorCode.TICKET_STATE_CONFLICT);
        }
        LocalDateTime expiresAt = request.expiresAt();
        if (!expiresAt.isAfter(ticket.getActivatedAt())) {
            throw new CustomException(ErrorCode.FUNDING_INVALID_REQUEST);
        }
        LocalDateTime now = LocalDateTime.now(KST);
        if (ticket.getStatus() == TicketStatus.EXPIRED && expiresAt.isAfter(now)) {
            classTicketRepository.findByRegisteredUserIdAndStatus(ticket.getRegisteredUser().getId(), TicketStatus.ACTIVE)
                    .filter(other -> !other.getId().equals(ticket.getId()))
                    .ifPresent(other -> {
                        throw new CustomException(ErrorCode.TICKET_ALREADY_ACTIVE);
                    });
        }
        LocalDateTime previous = ticket.getExpiresAt();
        ticket.changeExpiresAt(expiresAt, now);
        adminAuditLogRepository.save(new AdminAuditLog(adminUserId, "TICKET_EXPIRES_AT_CHANGED", "CLASS_TICKET",
                ticket.getId(), null, "{\"previous\":\"" + previous + "\",\"expiresAt\":\"" + expiresAt + "\"}"));
        return new TicketExpiresAtResponse(ticket.getId(), ticket.getActivatedAt(), previous, ticket.getExpiresAt(),
                ticket.getReadGraceEndsAt(), ticket.getStatus());
    }

    // ───────────── 3. 전달 기록 ─────────────

    @Transactional
    public TicketDeliveryResponse recordDelivery(Long ticketId, TicketDeliveryRequest request) {
        ClassTicket ticket = getTicket(ticketId);
        if (!ticket.isUnclaimed()) {
            throw new CustomException(ErrorCode.TICKET_STATE_CONFLICT);
        }
        LocalDateTime now = LocalDateTime.now(KST);
        TicketDeliveryAttempt attempt = ticketDeliveryAttemptRepository.save(TicketDeliveryAttempt.builder()
                .ticket(ticket)
                .channel(request.channel())
                .status(request.status())
                .failureReason(request.status() == DeliveryStatus.FAILED ? request.failureReason() : null)
                .attemptedAt(now)
                .build());
        if (request.status() == DeliveryStatus.SUCCESS) {
            ticket.markDelivered(now);
        }
        return new TicketDeliveryResponse(attempt.getId(), ticket.getId(), attempt.getChannel(), attempt.getStatus(),
                attempt.getFailureReason(), attempt.getAttemptedAt(), ticket.getCodeStatus(), ticket.getDeliveredAt());
    }

    @Transactional(readOnly = true)
    public FundingOrderSummaryResponse getOrderDetail(Long fundingOrderId) {
        FundingOrder order = fundingOrderRepository.findById(fundingOrderId)
                .orElseThrow(() -> new CustomException(ErrorCode.FUNDING_ORDER_NOT_FOUND));
        List<ClassTicket> tickets = classTicketRepository.findAllByFundingOrderIdAndStatusNot(order.getId(), TicketStatus.REVOKED);
        List<Long> ticketIds = tickets.stream().map(ClassTicket::getId).toList();
        Map<Long, DeliveryStatus> deliveries = ticketIds.isEmpty() ? Map.of()
                : ticketDeliveryAttemptRepository.findByTicketIdInOrderByIdDesc(ticketIds).stream()
                        .collect(Collectors.toMap(a -> a.getTicket().getId(), TicketDeliveryAttempt::getStatus,
                                (latest, ignored) -> latest));
        TumblbugOrderDetailResponse tumblbug = tumblbugImportRecordService.getDetails(List.of(order.getId())).get(order.getId());
        return toSummary(order, tickets, tumblbug, deliveries);
    }

    @Transactional
    public FundingOrderSummaryResponse updateOrderContact(Long fundingOrderId, Long adminUserId,
                                                          FundingOrderContactUpdateRequest request) {
        FundingOrder order = fundingOrderRepository.findByIdForUpdate(fundingOrderId)
                .orElseThrow(() -> new CustomException(ErrorCode.FUNDING_ORDER_NOT_FOUND));
        String beforeName = order.getSupporterName();
        String beforeEmail = order.getSupporterEmail();
        String beforePhone = order.getSupporterPhone();
        String name = request.supporterName().strip();
        String email = request.supporterEmail() == null || request.supporterEmail().isBlank()
                ? null : request.supporterEmail().strip();
        String phone = request.supporterPhone() == null || request.supporterPhone().isBlank()
                ? null : request.supporterPhone().strip();
        order.updateSupporterContact(name, email, phone);
        if (!java.util.Objects.equals(beforeName, name) || !java.util.Objects.equals(beforeEmail, email)
                || !java.util.Objects.equals(beforePhone, phone)) {
            Map<String, Object> before = new HashMap<>();
            before.put("name", beforeName);
            before.put("email", beforeEmail);
            before.put("phone", beforePhone);
            Map<String, Object> after = new HashMap<>();
            after.put("name", name);
            after.put("email", email);
            after.put("phone", phone);
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("before", before);
            metadata.put("after", after);
            try {
                adminAuditLogRepository.save(new AdminAuditLog(adminUserId, "FUNDING_ORDER_CONTACT_UPDATED",
                        "FUNDING_ORDER", order.getId(), "후원자 연락처 수정", objectMapper.writeValueAsString(metadata)));
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Funding order audit metadata could not be serialized", e);
            }
        }
        return getOrderDetail(fundingOrderId);
    }

    @Transactional
    public FundingOrderSummaryResponse updateOrderAdminFields(Long fundingOrderId, Long adminUserId,
                                                               FundingOrderAdminUpdateRequest request) {
        FundingOrder order = fundingOrderRepository.findByIdForUpdate(fundingOrderId)
                .orElseThrow(() -> new CustomException(ErrorCode.FUNDING_ORDER_NOT_FOUND));
        String nextOrderId = request.orderId().strip();
        if (fundingOrderRepository.existsByOrderIdAndIdNot(nextOrderId, order.getId())) {
            throw new CustomException(ErrorCode.COMMON_INVALID_PARAMETER);
        }
        ContactFields contact = parseContact(request.contact());
        String nextName = request.supporterName().strip();
        List<ClassTicket> tickets = classTicketRepository.findAllByFundingOrderIdAndStatusNotForUpdate(
                order.getId(), TicketStatus.REVOKED);
        if (request.quantity() < tickets.size()) {
            throw new CustomException(ErrorCode.TICKET_STATE_CONFLICT);
        }

        var actualRegistrationStatus = appRegistrationStatus(tickets);
        if (request.appRegistrationStatus() != actualRegistrationStatus) {
            throw new CustomException(ErrorCode.TICKET_STATE_CONFLICT);
        }

        boolean rewardChanged = order.getRewardTier() != request.rewardTier();
        if (rewardChanged) {
            for (ClassTicket ticket : tickets) {
                if (ticket.getStatus() != TicketStatus.UNREGISTERED || ticket.getRegisteredUser() != null
                        || !ticket.isUnclaimed() || classroomRepository.existsByTicketId(ticket.getId())) {
                    throw new CustomException(ErrorCode.TICKET_STATE_CONFLICT);
                }
            }
        }

        Map<Long, DeliveryStatus> latestDeliveryByTicket = latestDeliveryStatuses(tickets);
        var currentDeliveryStatus = orderDeliveryStatus(tickets, latestDeliveryByTicket);
        if (request.deliveryStatus() != currentDeliveryStatus) {
            if (request.deliveryStatus() == com.swyp.picke.domain.funding.enums.DeliveryStatus.PENDING
                    || tickets.isEmpty() || tickets.stream().anyMatch(ticket -> !ticket.isUnclaimed())) {
                throw new CustomException(ErrorCode.TICKET_STATE_CONFLICT);
            }
        }

        Map<String, Object> before = new HashMap<>();
        before.put("supporterName", order.getSupporterName());
        before.put("supporterEmail", order.getSupporterEmail());
        before.put("supporterPhone", order.getSupporterPhone());
        before.put("orderId", order.getOrderId());
        before.put("rewardTier", order.getRewardTier());
        before.put("quantity", order.getQuantity());
        before.put("deliveryStatus", currentDeliveryStatus);
        before.put("appRegistrationStatus", actualRegistrationStatus);

        boolean fieldsChanged = !java.util.Objects.equals(order.getSupporterName(), nextName)
                || !java.util.Objects.equals(order.getSupporterEmail(), contact.email())
                || !java.util.Objects.equals(order.getSupporterPhone(), contact.phone())
                || !java.util.Objects.equals(order.getOrderId(), nextOrderId)
                || order.getRewardTier() != request.rewardTier()
                || order.getQuantity() != request.quantity()
                || request.deliveryStatus() != currentDeliveryStatus;

        order.updateSupporterContact(nextName, contact.email(), contact.phone());
        order.updateAdminOrderFields(nextOrderId, request.rewardTier(), request.quantity());
        if (rewardChanged) {
            tickets.forEach(ticket -> ticket.updateCapacity(request.rewardTier().getCapacity()));
        }
        if (request.deliveryStatus() != currentDeliveryStatus) {
            LocalDateTime now = LocalDateTime.now(KST);
            DeliveryStatus recordedStatus = request.deliveryStatus() == com.swyp.picke.domain.funding.enums.DeliveryStatus.SENT
                    ? DeliveryStatus.SUCCESS : DeliveryStatus.FAILED;
            for (ClassTicket ticket : tickets) {
                ticketDeliveryAttemptRepository.save(TicketDeliveryAttempt.builder()
                        .ticket(ticket)
                        .channel(DeliveryChannel.EMAIL)
                        .status(recordedStatus)
                        .failureReason(recordedStatus == DeliveryStatus.FAILED ? "관리자 전달 상태 수정" : null)
                        .attemptedAt(now)
                        .build());
                if (recordedStatus == DeliveryStatus.SUCCESS) ticket.markDelivered(now);
            }
        }

        if (fieldsChanged) {
            Map<String, Object> after = new HashMap<>();
            after.put("supporterName", nextName);
            after.put("supporterEmail", contact.email());
            after.put("supporterPhone", contact.phone());
            after.put("orderId", nextOrderId);
            after.put("rewardTier", request.rewardTier());
            after.put("quantity", request.quantity());
            after.put("deliveryStatus", request.deliveryStatus());
            after.put("appRegistrationStatus", actualRegistrationStatus);
            try {
                adminAuditLogRepository.save(new AdminAuditLog(adminUserId, "FUNDING_ORDER_ADMIN_UPDATED",
                        "FUNDING_ORDER", order.getId(), "펀딩 구매자 정보 수정",
                        objectMapper.writeValueAsString(Map.of("before", before, "after", after))));
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Funding order audit metadata could not be serialized", e);
            }
        }
        return getOrderDetail(fundingOrderId);
    }

    private ContactFields parseContact(String value) {
        if (value == null || value.isBlank()) return new ContactFields(null, null);
        String email = null;
        String phone = null;
        String[] parts = value.strip().split("/", -1);
        if (parts.length > 2) throw new CustomException(ErrorCode.COMMON_INVALID_PARAMETER);
        for (String rawPart : parts) {
            String part = rawPart.strip();
            if (part.isBlank()) continue;
            if (part.contains("@")) {
                if (email != null || part.length() > 254 || !part.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                    throw new CustomException(ErrorCode.COMMON_INVALID_PARAMETER);
                }
                email = part;
            } else {
                if (phone != null || part.length() > 20 || !part.matches("^[+0-9() .-]+$")) {
                    throw new CustomException(ErrorCode.COMMON_INVALID_PARAMETER);
                }
                phone = part;
            }
        }
        return new ContactFields(email, phone);
    }

    private Map<Long, DeliveryStatus> latestDeliveryStatuses(List<ClassTicket> tickets) {
        List<Long> ticketIds = tickets.stream().map(ClassTicket::getId).toList();
        if (ticketIds.isEmpty()) return Map.of();
        return ticketDeliveryAttemptRepository.findByTicketIdInOrderByIdDesc(ticketIds).stream()
                .collect(Collectors.toMap(attempt -> attempt.getTicket().getId(), TicketDeliveryAttempt::getStatus,
                        (latest, ignored) -> latest));
    }

    private static FundingOrderAdminUpdateRequest.AppRegistrationStatus appRegistrationStatus(List<ClassTicket> tickets) {
        long registered = tickets.stream().filter(ticket -> ticket.getStatus() == TicketStatus.ACTIVE
                || ticket.getStatus() == TicketStatus.EXPIRED).count();
        if (tickets.isEmpty()) return FundingOrderAdminUpdateRequest.AppRegistrationStatus.NO_TICKET;
        if (registered == tickets.size()) return FundingOrderAdminUpdateRequest.AppRegistrationStatus.APP_REGISTERED;
        if (registered > 0) return FundingOrderAdminUpdateRequest.AppRegistrationStatus.APP_PARTIAL;
        return FundingOrderAdminUpdateRequest.AppRegistrationStatus.APP_PENDING;
    }

    private static com.swyp.picke.domain.funding.enums.DeliveryStatus orderDeliveryStatus(
            List<ClassTicket> tickets, Map<Long, DeliveryStatus> latestByTicket) {
        if (!tickets.isEmpty() && tickets.stream().allMatch(ticket -> latestByTicket.get(ticket.getId()) == DeliveryStatus.SUCCESS)) {
            return com.swyp.picke.domain.funding.enums.DeliveryStatus.SENT;
        }
        if (tickets.stream().anyMatch(ticket -> latestByTicket.get(ticket.getId()) == DeliveryStatus.FAILED)) {
            return com.swyp.picke.domain.funding.enums.DeliveryStatus.FAILED;
        }
        return com.swyp.picke.domain.funding.enums.DeliveryStatus.PENDING;
    }

    private record ContactFields(String email, String phone) { }

    // ───────────── 4. 재발급 ─────────────

    @Transactional
    public TicketIssueResponse.IssuedTicket reissue(Long ticketId) {
        ClassTicket previous = getTicket(ticketId);
        if (previous.isRevoked()) {
            throw new CustomException(ErrorCode.TICKET_CODE_REVOKED);
        }
        if (previous.getCodeStatus() == TicketCodeStatus.CLAIMED) {
            throw new CustomException(ErrorCode.TICKET_CODE_ALREADY_CLAIMED);
        }
        if (previous.getStatus() == TicketStatus.REGISTER_EXPIRED) {
            throw new CustomException(ErrorCode.TICKET_REGISTER_EXPIRED);
        }
        LocalDateTime now = LocalDateTime.now(KST);
        previous.revoke(now);
        // 살아 있는 행은 주문당 1개(partial unique). Hibernate 는 INSERT 를 UPDATE 보다 먼저 내보내므로 먼저 flush 한다.
        classTicketRepository.flush();

        String rawCode = generateUniqueCode();
        ClassTicket reissued = classTicketRepository.save(
                ClassTicket.reissueFrom(previous, TicketCodeHasher.hash(rawCode),
                        ticketCodeVault.encrypt(TicketCodeGenerator.format(rawCode))));
        reissued.recordCodeSuffix(rawCode);
        return toIssued(reissued, rawCode);
    }

    // ───────────── 5. 환불 ─────────────

    @Transactional
    public FundingRefundResponse refund(FundingRefundRequest request) {
        List<Long> ids = request.fundingOrderIds().stream().distinct().toList();
        List<FundingOrder> orders = fundingOrderRepository.findAllById(ids);
        if (orders.size() != ids.size()) {
            throw new CustomException(ErrorCode.FUNDING_ORDER_NOT_FOUND);
        }
        Map<Long, List<ClassTicket>> ticketsByOrder = classTicketRepository.findByFundingOrderIdInAndStatusNot(ids, TicketStatus.REVOKED)
                .stream().collect(Collectors.groupingBy(ticket -> ticket.getFundingOrder().getId()));
        LocalDateTime now = LocalDateTime.now(KST);
        int refunded = 0;
        int revokedTickets = 0;
        List<Long> alreadyRefunded = new ArrayList<>();
        for (FundingOrder order : orders) {
            if (order.getPaymentStatus() == PaymentStatus.REFUNDED) {
                alreadyRefunded.add(order.getId());
                continue;
            }
            order.updatePayment(PaymentStatus.REFUNDED, order.getPaidAt());
            refunded++;
            for (ClassTicket live : ticketsByOrder.getOrDefault(order.getId(), List.of())) {
                live.revoke(now);
                revokedTickets++;
            }
        }
        return new FundingRefundResponse(refunded, revokedTickets, alreadyRefunded);
    }

    // ───────────── 5-1. 등록기한 임박 알림 ─────────────

    /**
     * 미등록 이용권은 앱 계정에 연결되지 않아 푸시를 보낼 수 없다. 코드 전달과 같이 운영자가 허용된 연락 수단으로 보내고 결과를 기록한다.
     * 대상은 조회 시점 기준으로 계산하므로 별도 배치가 없다.
     */
    public RegisterReminderTargetResponse getRegisterReminderTargets(RegisterReminderStage stage) {
        LocalDateTime now = LocalDateTime.now(KST);
        List<RegisterReminderTargetResponse.Item> items = classTicketRepository
                .findRegisterReminderTargets(stage.windowStart(now), stage.windowEnd(now), stage).stream()
                .map(ticket -> toReminderTarget(ticket, now))
                .toList();
        return new RegisterReminderTargetResponse(stage, items.size(), items);
    }

    @Transactional
    public RegisterReminderResponse recordRegisterReminder(Long ticketId, RegisterReminderRequest request) {
        ClassTicket ticket = getTicket(ticketId);
        if (ticket.getStatus() != TicketStatus.UNREGISTERED || ticket.getCodeStatus() != TicketCodeStatus.DELIVERED) {
            throw new CustomException(ErrorCode.TICKET_STATE_CONFLICT);
        }
        LocalDateTime now = LocalDateTime.now(KST);
        if (!ticket.isRegisterWindowOpen(now)) {
            throw new CustomException(ErrorCode.TICKET_REGISTER_EXPIRED);
        }
        TicketRegisterReminder reminder = ticketRegisterReminderRepository.save(TicketRegisterReminder.builder()
                .ticket(ticket)
                .stage(request.stage())
                .channel(request.channel())
                .status(request.status())
                .failureReason(request.status() == DeliveryStatus.FAILED ? request.failureReason() : null)
                .attemptedAt(now)
                .build());
        return new RegisterReminderResponse(reminder.getId(), ticket.getId(), reminder.getStage(), reminder.getChannel(),
                reminder.getStatus(), reminder.getFailureReason(), reminder.getAttemptedAt());
    }

    private static RegisterReminderTargetResponse.Item toReminderTarget(ClassTicket ticket, LocalDateTime now) {
        FundingOrder order = ticket.getFundingOrder();
        return new RegisterReminderTargetResponse.Item(ticket.getId(), order.getId(), order.getPlatform(),
                order.getOrderId(), order.getSupporterName(), order.getSupporterEmail(), order.getSupporterPhone(),
                ticket.getCapacity(), ticket.getRegisterBy(),
                ChronoUnit.DAYS.between(now.toLocalDate(), ticket.getRegisterBy().toLocalDate()));
    }

    private long countRegisterReminderTargets(RegisterReminderStage stage, LocalDateTime now) {
        return classTicketRepository.countRegisterReminderTargets(stage.windowStart(now), stage.windowEnd(now), stage);
    }

    // ───────────── 6. 조회 ─────────────

    public FundingOrderPageResponse getOrders(String campaignId, PaymentStatus paymentStatus, TicketStatus ticketStatus,
                                              TicketCodeStatus codeStatus, String keyword, Integer capacity,
                                              com.swyp.picke.domain.funding.enums.DeliveryStatus deliveryStatus,
                                              int page, int size) {
        int pageNumber = Math.max(page, 0);
        int pageSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        String normalizedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        String normalizedCampaignId = (campaignId == null || campaignId.isBlank()) ? null : campaignId.trim();
        RewardTier rewardTier = null;
        if (capacity != null) {
            try {
                rewardTier = RewardTier.valueOf("SEAT_" + capacity);
            } catch (IllegalArgumentException e) {
                throw new CustomException(ErrorCode.FUNDING_REWARD_UNSUPPORTED);
            }
        }
        // 전달 상태는 boolean 으로 넘긴다. enum 파라미터를 상수와 비교하면 Hibernate 가 파라미터 타입을 정하지 못해 500 이 난다 (#612)
        boolean pending = deliveryStatus == com.swyp.picke.domain.funding.enums.DeliveryStatus.PENDING;
        boolean sent = deliveryStatus == com.swyp.picke.domain.funding.enums.DeliveryStatus.SENT;
        boolean failed = deliveryStatus == com.swyp.picke.domain.funding.enums.DeliveryStatus.FAILED;

        Page<FundingOrder> result = fundingOrderRepository.searchOrders(
                paymentStatus, ticketStatus, codeStatus, normalizedCampaignId, normalizedKeyword, rewardTier,
                sent, failed, pending, PageRequest.of(pageNumber, pageSize));
        List<Long> orderIds = result.getContent().stream().map(FundingOrder::getId).toList();
        Map<Long, TumblbugOrderDetailResponse> tumblbugDetails = tumblbugImportRecordService.getDetails(orderIds);
        Map<Long, List<ClassTicket>> liveTicketsByOrder = orderIds.isEmpty() ? Map.of()
                : classTicketRepository.findByFundingOrderIdInAndStatusNotOrderByIdAsc(orderIds, TicketStatus.REVOKED)
                        .stream().collect(Collectors.groupingBy(t -> t.getFundingOrder().getId()));
        List<Long> ticketIds = liveTicketsByOrder.values().stream().flatMap(List::stream).map(ClassTicket::getId).toList();
        Map<Long, DeliveryStatus> deliveries = ticketIds.isEmpty() ? Map.of()
                : ticketDeliveryAttemptRepository.findByTicketIdInOrderByIdDesc(ticketIds).stream()
                        .collect(Collectors.toMap(a -> a.getTicket().getId(), TicketDeliveryAttempt::getStatus,
                                (latest, ignored) -> latest));
        List<FundingOrderSummaryResponse> items = result.getContent().stream()
                .map(order -> toSummary(order, liveTicketsByOrder.getOrDefault(order.getId(), List.of()),
                        tumblbugDetails.get(order.getId()), deliveries))
                .toList();
        return new FundingOrderPageResponse(items, pageNumber, pageSize, result.getTotalElements(), result.hasNext());
    }

    public FundingSummaryResponse getSummary() {
        LocalDateTime now = LocalDateTime.now(KST);
        return new FundingSummaryResponse(
                fundingOrderRepository.count(),
                fundingOrderRepository.countByPaymentStatus(PaymentStatus.PAID_CONFIRMED),
                fundingOrderRepository.countIssuable(),
                fundingOrderRepository.countHeld(),
                classTicketRepository.countByStatusNot(TicketStatus.REVOKED),
                classTicketRepository.countByCodeStatus(TicketCodeStatus.ISSUED),
                classTicketRepository.countByCodeStatus(TicketCodeStatus.DELIVERED),
                classTicketRepository.countByStatusIn(List.of(TicketStatus.ACTIVE, TicketStatus.EXPIRED)),
                fundingOrderRepository.countByPaymentStatus(PaymentStatus.REFUNDED),
                fundingOrderRepository.countByPaymentStatus(PaymentStatus.CANCELED),
                countRegisterReminderTargets(RegisterReminderStage.D30, now),
                countRegisterReminderTargets(RegisterReminderStage.D7, now));
    }

    public TumblbugCampaignDashboardResponse getTumblbugCampaignDashboard(String campaignId) {
        String normalizedCampaignId = campaignId == null ? "" : campaignId.trim();
        if (normalizedCampaignId.isBlank() || normalizedCampaignId.length() > 50) {
            throw new CustomException(ErrorCode.COMMON_INVALID_PARAMETER);
        }
        long total = fundingOrderRepository.countByPlatformAndCampaignId(
                FundingPlatform.TUMBLBUG, normalizedCampaignId);
        long paid = fundingOrderRepository.countByPlatformAndCampaignIdAndPaymentStatus(
                FundingPlatform.TUMBLBUG, normalizedCampaignId, PaymentStatus.PAID_CONFIRMED);
        long delivered = fundingOrderRepository.countDeliveredByTumblbugCampaign(normalizedCampaignId);
        long issuedCodes = classTicketRepository.countLiveTumblbugTickets(normalizedCampaignId);
        long deliveredCodes = classTicketRepository.countDeliveredTumblbugTickets(normalizedCampaignId);
        return new TumblbugCampaignDashboardResponse(normalizedCampaignId, total, paid, delivered, total - delivered,
                issuedCodes, deliveredCodes, issuedCodes - deliveredCodes);
    }

    private static FundingOrderSummaryResponse toSummary(FundingOrder order, List<ClassTicket> tickets,
                                                         TumblbugOrderDetailResponse tumblbug,
                                                         Map<Long, DeliveryStatus> deliveries) {
        List<FundingOrderSummaryResponse.Ticket> ticketViews = new ArrayList<>();
        for (ClassTicket ticket : tickets) {
            User registrant = ticket.getRegisteredUser();
            DeliveryStatus latestDelivery = deliveries.get(ticket.getId());
            ticketViews.add(new FundingOrderSummaryResponse.Ticket(ticket.getId(), ticket.getCapacity(),
                    ticket.getCodeStatus(), ticket.getStatus(), ticket.getDeliveredAt(), ticket.getRegisterBy(),
                    ticket.getActivatedAt(), ticket.getExpiresAt(),
                    registrant == null ? null : registrant.getId(),
                    registrant == null ? null : registrant.getNickname(), ticket.getCodeSuffix(),
                    toDeliveryView(latestDelivery)));
        }
        boolean issuable = order.isIssuable() && ticketViews.size() < order.getQuantity();
        return new FundingOrderSummaryResponse(order.getId(), order.getPlatform(), order.getCampaignId(),
                order.getOrderId(), order.getRewardTier(), order.getQuantity(), order.getSupporterName(),
                order.getSupporterEmail(), order.getSupporterPhone(), order.getPaymentStatus(), order.getPaidAt(),
                issuable, ticketViews.isEmpty() ? null : ticketViews.getFirst(), List.copyOf(ticketViews), tumblbug,
                orderDeliveryView(ticketViews));
    }

    /** 이용권의 마지막 전달 기록. 기록이 없으면 PENDING */
    private static com.swyp.picke.domain.funding.enums.DeliveryStatus toDeliveryView(DeliveryStatus latest) {
        if (latest == DeliveryStatus.SUCCESS) return com.swyp.picke.domain.funding.enums.DeliveryStatus.SENT;
        if (latest == DeliveryStatus.FAILED) return com.swyp.picke.domain.funding.enums.DeliveryStatus.FAILED;
        return com.swyp.picke.domain.funding.enums.DeliveryStatus.PENDING;
    }

    /** 주문 단위 전달 상태. 목록 필터(searchOrders)와 같은 기준: 모두 SENT 면 SENT, 하나라도 FAILED 면 FAILED (#612) */
    private static com.swyp.picke.domain.funding.enums.DeliveryStatus orderDeliveryView(
            List<FundingOrderSummaryResponse.Ticket> tickets) {
        if (!tickets.isEmpty() && tickets.stream().allMatch(t ->
                t.deliveryStatus() == com.swyp.picke.domain.funding.enums.DeliveryStatus.SENT)) {
            return com.swyp.picke.domain.funding.enums.DeliveryStatus.SENT;
        }
        if (tickets.stream().anyMatch(t -> t.deliveryStatus() == com.swyp.picke.domain.funding.enums.DeliveryStatus.FAILED)) {
            return com.swyp.picke.domain.funding.enums.DeliveryStatus.FAILED;
        }
        return com.swyp.picke.domain.funding.enums.DeliveryStatus.PENDING;
    }

    @Transactional
    public AdminTicketCodeResponse getTicketCode(Long ticketId, Long adminUserId) {
        ClassTicket ticket = getTicket(ticketId);
        if (ticket.getEncryptedCode() == null || ticket.getEncryptedCode().isBlank()) {
            throw new CustomException(ErrorCode.TICKET_CODE_NOT_RETRIEVABLE);
        }
        adminAuditLogRepository.save(new AdminAuditLog(adminUserId, "TICKET_CODE_VIEWED", "CLASS_TICKET",
                ticketId, null, "{}"));
        return new AdminTicketCodeResponse(ticketId, ticketCodeVault.decrypt(ticket.getEncryptedCode()));
    }

    // ───────────── 공통 ─────────────

    private ClassTicket getTicket(Long ticketId) {
        return classTicketRepository.findById(ticketId)
                .orElseThrow(() -> new CustomException(ErrorCode.TICKET_NOT_FOUND));
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}

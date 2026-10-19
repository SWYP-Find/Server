package com.swyp.picke.domain.admin.service;

import com.swyp.picke.domain.admin.dto.funding.request.FundingRefundRequest;
import com.swyp.picke.domain.admin.dto.funding.request.TicketDeliveryRequest;
import com.swyp.picke.domain.admin.dto.funding.request.TicketIssueRequest;
import com.swyp.picke.domain.admin.dto.funding.response.FundingImportResponse;
import com.swyp.picke.domain.admin.dto.funding.response.FundingOrderPageResponse;
import com.swyp.picke.domain.admin.dto.funding.response.FundingOrderSummaryResponse;
import com.swyp.picke.domain.admin.dto.funding.response.FundingRefundResponse;
import com.swyp.picke.domain.admin.dto.funding.response.FundingSummaryResponse;
import com.swyp.picke.domain.admin.dto.funding.response.TicketDeliveryResponse;
import com.swyp.picke.domain.admin.dto.funding.response.TicketIssueResponse;
import com.swyp.picke.domain.ticket.entity.ClassTicket;
import com.swyp.picke.domain.ticket.entity.FundingOrder;
import com.swyp.picke.domain.ticket.entity.TicketDeliveryAttempt;
import com.swyp.picke.domain.ticket.enums.DeliveryStatus;
import com.swyp.picke.domain.ticket.enums.PaymentStatus;
import com.swyp.picke.domain.ticket.enums.TicketCodeStatus;
import com.swyp.picke.domain.ticket.enums.TicketStatus;
import com.swyp.picke.domain.ticket.repository.ClassTicketRepository;
import com.swyp.picke.domain.ticket.repository.FundingOrderRepository;
import com.swyp.picke.domain.ticket.repository.TicketDeliveryAttemptRepository;
import com.swyp.picke.domain.ticket.util.FundingCsvParser;
import com.swyp.picke.domain.ticket.util.TicketCodeGenerator;
import com.swyp.picke.domain.ticket.util.TicketCodeHasher;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
 *   <li>발급: 결제 확정·수량 1·미발급 주문에 코드 생성. 코드 원문은 응답에서 1회만 노출, 서버는 SHA-256 만 저장.</li>
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
    private final ClassTicketRepository classTicketRepository;
    private final TicketDeliveryAttemptRepository ticketDeliveryAttemptRepository;

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
        Set<Long> orderIdsWithLiveTicket = classTicketRepository
                .findByFundingOrderIdInAndStatusNot(
                        existingByOrderId.values().stream().map(FundingOrder::getId).toList(), TicketStatus.REVOKED)
                .stream().map(t -> t.getFundingOrder().getId()).collect(Collectors.toSet());

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
            if (orderIdsWithLiveTicket.contains(existing.getId()) && existing.getRewardTier() != row.rewardTier()) {
                errors.add(new FundingImportResponse.RowError(row.line(), row.orderId(),
                        "이용권이 이미 발급된 주문의 reward_tier 는 변경할 수 없습니다. 환불 처리 후 다시 올려 주세요."));
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

        List<FundingOrder> targets;
        if (request == null || request.fundingOrderIds() == null || request.fundingOrderIds().isEmpty()) {
            targets = fundingOrderRepository.findIssuable();
        } else {
            List<Long> ids = request.fundingOrderIds().stream().distinct().toList();
            Map<Long, FundingOrder> found = fundingOrderRepository.findAllById(ids).stream()
                    .collect(Collectors.toMap(FundingOrder::getId, Function.identity()));
            Set<Long> alreadyIssued = classTicketRepository.findByFundingOrderIdInAndStatusNot(ids, TicketStatus.REVOKED)
                    .stream().map(t -> t.getFundingOrder().getId()).collect(Collectors.toSet());
            targets = new ArrayList<>();
            for (Long id : ids) {
                FundingOrder order = found.get(id);
                if (order == null) {
                    skipped.add(new TicketIssueResponse.Skipped(id, null, "NOT_FOUND"));
                } else if (alreadyIssued.contains(id)) {
                    skipped.add(new TicketIssueResponse.Skipped(id, order.getOrderId(), "ALREADY_ISSUED"));
                } else if (!order.isIssuable() || order.getPaidAt() == null) {
                    skipped.add(new TicketIssueResponse.Skipped(id, order.getOrderId(), "NOT_ISSUABLE"));
                } else {
                    targets.add(order);
                }
            }
        }

        for (FundingOrder order : targets) {
            String rawCode = generateUniqueCode();
            ClassTicket ticket = classTicketRepository.save(ClassTicket.issue(order, TicketCodeHasher.hash(rawCode)));
            issued.add(toIssued(ticket, rawCode));
        }
        return new TicketIssueResponse(issued, skipped);
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
                ClassTicket.reissueFrom(previous, TicketCodeHasher.hash(rawCode)));
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
            ClassTicket live = classTicketRepository
                    .findByFundingOrderIdAndStatusNot(order.getId(), TicketStatus.REVOKED).orElse(null);
            if (live != null) {
                live.revoke(now);
                revokedTickets++;
            }
        }
        return new FundingRefundResponse(refunded, revokedTickets, alreadyRefunded);
    }

    // ───────────── 6. 조회 ─────────────

    public FundingOrderPageResponse getOrders(PaymentStatus paymentStatus, TicketStatus ticketStatus,
                                              TicketCodeStatus codeStatus, String keyword, int page, int size) {
        int pageNumber = Math.max(page, 0);
        int pageSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        String normalizedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();

        Page<Object[]> result = fundingOrderRepository.searchWithLiveTicket(
                paymentStatus, ticketStatus, codeStatus, normalizedKeyword, PageRequest.of(pageNumber, pageSize));
        List<FundingOrderSummaryResponse> items = result.getContent().stream()
                .map(row -> toSummary((FundingOrder) row[0], (ClassTicket) row[1]))
                .toList();
        return new FundingOrderPageResponse(items, pageNumber, pageSize, result.getTotalElements(), result.hasNext());
    }

    public FundingSummaryResponse getSummary() {
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
                fundingOrderRepository.countByPaymentStatus(PaymentStatus.CANCELED));
    }

    private static FundingOrderSummaryResponse toSummary(FundingOrder order, ClassTicket ticket) {
        FundingOrderSummaryResponse.Ticket ticketView = null;
        if (ticket != null) {
            User registrant = ticket.getRegisteredUser();
            ticketView = new FundingOrderSummaryResponse.Ticket(ticket.getId(), ticket.getCapacity(),
                    ticket.getCodeStatus(), ticket.getStatus(), ticket.getDeliveredAt(), ticket.getRegisterBy(),
                    ticket.getActivatedAt(), ticket.getExpiresAt(),
                    registrant == null ? null : registrant.getId(),
                    registrant == null ? null : registrant.getNickname());
        }
        boolean issuable = ticket == null && order.isIssuable() && order.getPaidAt() != null;
        return new FundingOrderSummaryResponse(order.getId(), order.getPlatform(), order.getCampaignId(),
                order.getOrderId(), order.getRewardTier(), order.getQuantity(), order.getSupporterName(),
                order.getSupporterEmail(), order.getSupporterPhone(), order.getPaymentStatus(), order.getPaidAt(),
                issuable, ticketView);
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

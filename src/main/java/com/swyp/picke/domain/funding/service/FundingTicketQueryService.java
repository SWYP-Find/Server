package com.swyp.picke.domain.funding.service;

import com.swyp.picke.domain.admin.service.AdminClassUsageService;
import com.swyp.picke.domain.funding.dto.response.AdminTicketSort;
import com.swyp.picke.domain.funding.dto.response.TicketAdminRowResponse;
import com.swyp.picke.domain.funding.dto.response.TicketAdminDetailResponse;
import com.swyp.picke.domain.classroom.repository.ClassMemberRepository;
import com.swyp.picke.domain.classroom.repository.ClassroomRepository;
import com.swyp.picke.domain.ticket.entity.ClassTicket;
import com.swyp.picke.domain.ticket.entity.FundingOrder;
import com.swyp.picke.domain.ticket.entity.TicketDeliveryAttempt;
import com.swyp.picke.domain.admin.dto.classroom.response.AdminClassState;
import com.swyp.picke.domain.ticket.enums.TicketStatus;
import com.swyp.picke.domain.ticket.repository.ClassTicketRepository;
import com.swyp.picke.domain.ticket.repository.TicketDeliveryAttemptRepository;
import com.swyp.picke.domain.ticket.util.TicketCodeHasher;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import com.swyp.picke.global.common.response.PageResponse;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Ticket/registrant search only; order lists belong to AdminFundingService. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FundingTicketQueryService {
    private final ClassTicketRepository classTicketRepository;
    private final TicketDeliveryAttemptRepository deliveryAttemptRepository;
    private final ClassroomRepository classroomRepository;
    private final ClassMemberRepository classMemberRepository;
    private final AdminClassUsageService adminClassUsageService;

    public TicketAdminDetailResponse getTicketDetail(Long ticketId) {
        ClassTicket ticket = classTicketRepository.findById(ticketId)
                .orElseThrow(() -> new CustomException(ErrorCode.TICKET_NOT_FOUND));
        FundingOrder order = ticket.getFundingOrder();
        var registeredUser = ticket.getRegisteredUser();
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        List<TicketAdminDetailResponse.Delivery> deliveries = deliveryAttemptRepository
                .findByTicketIdOrderByAttemptedAtDesc(ticketId).stream()
                .map(this::toDelivery).toList();
        List<TicketAdminDetailResponse.ClassItem> classes = classroomRepository.findAdminByTicketId(ticketId).stream()
                .map(classroom -> new TicketAdminDetailResponse.ClassItem(classroom.getId(), classroom.getName(),
                        (int) classMemberRepository.countByClassroomIdAndSeatHeldTrue(classroom.getId()),
                        classroom.getCapacity(), classroom.isClosed(now) ? AdminClassState.CLOSED : AdminClassState.OPEN,
                        classroom.getCreatedAt()))
                .toList();
        return new TicketAdminDetailResponse(ticket.getId(), order.getId(),
                ticket.getCodeSuffix() == null ? null : "****-" + ticket.getCodeSuffix(),
                ticket.getEncryptedCode() != null && !ticket.getEncryptedCode().isBlank(),
                order.getSupporterName(), order.getSupporterEmail(), order.getSupporterPhone(),
                registeredUser == null ? null : registeredUser.getId(),
                registeredUser == null ? null : registeredUser.getNickname(),
                order.getPlatform(), order.getCampaignId(), order.getOrderId(), order.getRewardTier(),
                order.getQuantity(), ticket.getCapacity(), order.getPaymentStatus(),
                effectiveStatus(ticket, now), ticket.getCodeStatus(), ticket.getCreatedAt(), ticket.getDeliveredAt(),
                ticket.getRegisterBy(), ticket.getActivatedAt(), ticket.getExpiresAt(), ticket.getRevokedAt(),
                deliveries, classes,
                registeredUser == null ? null : registeredUser.getUserTag(),
                registeredUser == null ? null : adminClassUsageService.accountEmails(List.of(registeredUser.getId()))
                        .get(registeredUser.getId()),
                ticket.getUsageDays());
    }

    private TicketAdminDetailResponse.Delivery toDelivery(TicketDeliveryAttempt attempt) {
        return new TicketAdminDetailResponse.Delivery(attempt.getChannel(), attempt.getStatus(),
                attempt.getFailureReason(), attempt.getAttemptedAt());
    }

    public PageResponse<TicketAdminRowResponse> searchTickets(String keyword, Integer capacity, TicketStatus status,
            LocalDate registeredFrom, LocalDate registeredTo, int page, int size) {
        return searchTickets(keyword, capacity, status, registeredFrom, registeredTo, AdminTicketSort.LATEST, page, size);
    }

    public PageResponse<TicketAdminRowResponse> searchTickets(String keyword, Integer capacity, TicketStatus status,
            LocalDate registeredFrom, LocalDate registeredTo, AdminTicketSort sort, int page, int size) {
        if (registeredFrom != null && registeredTo != null && registeredFrom.isAfter(registeredTo)) {
            throw new CustomException(ErrorCode.COMMON_INVALID_PARAMETER);
        }
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        var result = classTicketRepository.findAll((root, query, cb) -> {
            List<Predicate> filters = new ArrayList<>();
            if (capacity != null) filters.add(cb.equal(root.get("capacity"), capacity));
            if (status != null) filters.add(status(root, status, now, cb));
            if (registeredFrom != null) filters.add(cb.greaterThanOrEqualTo(root.get("activatedAt"), registeredFrom.atStartOfDay()));
            if (registeredTo != null) filters.add(cb.lessThan(root.get("activatedAt"), registeredTo.plusDays(1).atStartOfDay()));
            var order = root.join("fundingOrder");
            var owner = root.join("registeredUser", JoinType.LEFT);
            if (query != null && query.getResultType() != Long.class && query.getResultType() != long.class
                    && sort != null && sort != AdminTicketSort.LATEST) {
                var name = cb.coalesce(owner.<String>get("nickname"), order.<String>get("supporterName"));
                query.orderBy(sort == AdminTicketSort.NAME_DESC ? cb.desc(name) : cb.asc(name), cb.desc(root.get("id")));
            }
            if (keyword != null && !keyword.isBlank()) {
                String term = keyword.trim();
                String pattern = "%" + term.toLowerCase(Locale.ROOT) + "%";
                filters.add(cb.or(cb.like(cb.lower(order.get("supporterName")), pattern),
                        cb.like(cb.lower(order.get("orderId")), pattern),
                        cb.like(cb.lower(owner.get("nickname")), pattern),
                        cb.equal(order.get("supporterEmail"), term), cb.equal(order.get("supporterPhone"), term),
                        cb.equal(root.get("codeHash"), TicketCodeHasher.hash(term)),
                        cb.equal(root.get("codeSuffix"), TicketCodeHasher.normalize(term))));
            }
            return cb.and(filters.toArray(Predicate[]::new));
        }, pageable(page, size, sort));
        List<Long> ticketIds = result.getContent().stream().map(ClassTicket::getId).toList();
        var stats = adminClassUsageService.ticketListStats(ticketIds);
        var emails = adminClassUsageService.accountEmails(result.getContent().stream()
                .filter(ticket -> ticket.getRegisteredUser() != null)
                .map(ticket -> ticket.getRegisteredUser().getId()).distinct().toList());
        return PageResponse.of(result.map(ticket -> new TicketAdminRowResponse(ticket.getId(), ticket.getFundingOrder().getId(),
                ticket.getCodeSuffix() == null ? null : "****-" + ticket.getCodeSuffix(),
                ticket.getFundingOrder().getSupporterName(), mask(ticket.getFundingOrder()),
                ticket.getRegisteredUser() == null ? null : ticket.getRegisteredUser().getNickname(),
                ticket.getCapacity(), effectiveStatus(ticket, now), ticket.getCodeStatus(), ticket.getRegisterBy(),
                ticket.getActivatedAt(), ticket.getExpiresAt(),
                ticket.getEncryptedCode() != null && !ticket.getEncryptedCode().isBlank(),
                ticket.getRegisteredUser() == null ? null : ticket.getRegisteredUser().getId(),
                ticket.getRegisteredUser() == null ? null
                        : AdminClassUsageService.maskEmail(emails.get(ticket.getRegisteredUser().getId())),
                stats.get(ticket.getId()).classCount(), stats.get(ticket.getId()).lastActiveAt())));
    }

    private Pageable pageable(int page, int size, AdminTicketSort sort) {
        if (page < 0 || size < 1 || size > 100) throw new CustomException(ErrorCode.COMMON_INVALID_PARAMETER);
        // 이름순은 명세 안에서 정렬한다 (등록 계정 닉네임이 없으면 구매자 이름)
        return sort == null || sort == AdminTicketSort.LATEST
                ? PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"))
                : PageRequest.of(page, size);
    }

    private Predicate status(Root<ClassTicket> root, TicketStatus status, LocalDateTime now, CriteriaBuilder cb) {
        if (status == TicketStatus.ACTIVE) return cb.and(cb.equal(root.get("status"), status), cb.greaterThan(root.get("expiresAt"), now));
        if (status == TicketStatus.EXPIRED) return cb.or(cb.equal(root.get("status"), status),
                cb.and(cb.equal(root.get("status"), TicketStatus.ACTIVE), cb.lessThanOrEqualTo(root.get("expiresAt"), now)));
        if (status == TicketStatus.REGISTER_EXPIRED) return cb.or(cb.equal(root.get("status"), status),
                cb.and(cb.equal(root.get("status"), TicketStatus.UNREGISTERED), cb.lessThan(root.get("registerBy"), now)));
        if (status == TicketStatus.UNREGISTERED) return cb.and(cb.equal(root.get("status"), status), cb.greaterThanOrEqualTo(root.get("registerBy"), now));
        return cb.equal(root.get("status"), status);
    }

    private TicketStatus effectiveStatus(ClassTicket ticket, LocalDateTime now) {
        if (ticket.getStatus() == TicketStatus.ACTIVE && !ticket.isUsable(now)) return TicketStatus.EXPIRED;
        if (ticket.getStatus() == TicketStatus.UNREGISTERED && !ticket.isRegisterWindowOpen(now)) return TicketStatus.REGISTER_EXPIRED;
        return ticket.getStatus();
    }

    private String mask(FundingOrder order) {
        String email = order.getSupporterEmail();
        if (email != null && !email.isBlank()) {
            int at = email.indexOf('@');
            return at > 0 ? email.substring(0, 1) + "***" + email.substring(at) : "***";
        }
        String phone = order.getSupporterPhone();
        return phone == null || phone.isBlank() ? null : "***" + phone.substring(Math.max(0, phone.length() - 4));
    }
}

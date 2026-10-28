package com.swyp.picke.domain.funding.service;

import com.swyp.picke.domain.funding.dto.response.TicketAdminRowResponse;
import com.swyp.picke.domain.ticket.entity.ClassTicket;
import com.swyp.picke.domain.ticket.entity.FundingOrder;
import com.swyp.picke.domain.ticket.enums.TicketStatus;
import com.swyp.picke.domain.ticket.repository.ClassTicketRepository;
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

    public PageResponse<TicketAdminRowResponse> searchTickets(String keyword, Integer capacity, TicketStatus status,
            LocalDate registeredFrom, LocalDate registeredTo, int page, int size) {
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
            if (keyword != null && !keyword.isBlank()) {
                var order = root.join("fundingOrder");
                var owner = root.join("registeredUser", JoinType.LEFT);
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
        }, pageable(page, size));
        return PageResponse.of(result.map(ticket -> new TicketAdminRowResponse(ticket.getId(),
                ticket.getCodeSuffix() == null ? null : "****-" + ticket.getCodeSuffix(),
                ticket.getFundingOrder().getSupporterName(), mask(ticket.getFundingOrder()),
                ticket.getRegisteredUser() == null ? null : ticket.getRegisteredUser().getNickname(),
                ticket.getCapacity(), effectiveStatus(ticket, now), ticket.getCodeStatus(), ticket.getRegisterBy(),
                ticket.getActivatedAt(), ticket.getExpiresAt())));
    }

    private Pageable pageable(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new CustomException(ErrorCode.COMMON_INVALID_PARAMETER);
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
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

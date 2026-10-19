package com.swyp.picke.domain.ticket.service;

import com.swyp.picke.domain.ticket.converter.ClassTicketConverter;
import com.swyp.picke.domain.ticket.dto.request.ClassTicketRegisterRequest;
import com.swyp.picke.domain.ticket.dto.response.ClassTicketResponse;
import com.swyp.picke.domain.ticket.entity.ClassTicket;
import com.swyp.picke.domain.ticket.enums.TicketCodeStatus;
import com.swyp.picke.domain.ticket.enums.TicketStatus;
import com.swyp.picke.domain.ticket.repository.ClassTicketRepository;
import com.swyp.picke.domain.ticket.util.TicketCodeHasher;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.repository.UserRepository;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이용권 등록/조회.
 * 만료는 별도 스케줄러 없이 조회·등록 시점에 expiresAt 을 보고 지연 반영한다(lazy expire).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClassTicketServiceImpl implements ClassTicketService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final List<TicketStatus> MY_TICKET_STATUSES = List.of(TicketStatus.ACTIVE, TicketStatus.EXPIRED);

    private final ClassTicketRepository classTicketRepository;
    private final UserRepository userRepository;

    /**
     * 거절(CustomException)하면서도 상태 변경은 남겨야 하는 경우가 있어 롤백 대상에서 뺀다.
     * (등록 기한 경과 → REGISTER_EXPIRED 기록 후 409, 기간 끝난 ACTIVE → EXPIRED 기록)
     */
    @Override
    @Transactional(noRollbackFor = CustomException.class)
    public ClassTicketResponse register(Long userId, ClassTicketRegisterRequest request) {
        LocalDateTime now = LocalDateTime.now(KST);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        ClassTicket ticket = classTicketRepository.findByCodeHashForUpdate(TicketCodeHasher.hash(request.code()))
                .orElseThrow(() -> new CustomException(ErrorCode.TICKET_CODE_INVALID));

        validateCodeRegistrable(ticket, now);
        rejectIfUserHasActiveTicket(userId, now);

        ticket.register(user, now);
        log.info("클래스 이용권 등록 - ticketId: {}, userId: {}, expiresAt: {}", ticket.getId(), userId, ticket.getExpiresAt());
        return ClassTicketConverter.toResponse(ticket, now);
    }

    @Override
    @Transactional
    public ClassTicketResponse getMyTicket(Long userId) {
        LocalDateTime now = LocalDateTime.now(KST);
        ClassTicket ticket = classTicketRepository
                .findTopByRegisteredUserIdAndStatusInOrderByActivatedAtDesc(userId, MY_TICKET_STATUSES)
                .orElseThrow(() -> new CustomException(ErrorCode.TICKET_NOT_FOUND));

        expireIfNeeded(ticket, now);
        return ClassTicketConverter.toResponse(ticket, now);
    }

    private void validateCodeRegistrable(ClassTicket ticket, LocalDateTime now) {
        if (ticket.getCodeStatus() == TicketCodeStatus.REVOKED || ticket.getStatus() == TicketStatus.REVOKED) {
            throw new CustomException(ErrorCode.TICKET_CODE_REVOKED);
        }
        if (ticket.getCodeStatus() == TicketCodeStatus.CLAIMED || ticket.getStatus() != TicketStatus.UNREGISTERED) {
            // UNREGISTERED 가 아니면 이미 등록됐거나(ACTIVE/EXPIRED) 등록 기한이 끝난(REGISTER_EXPIRED) 코드
            if (ticket.getStatus() == TicketStatus.REGISTER_EXPIRED) {
                throw new CustomException(ErrorCode.TICKET_REGISTER_EXPIRED);
            }
            throw new CustomException(ErrorCode.TICKET_CODE_ALREADY_CLAIMED);
        }
        if (!ticket.isRegisterWindowOpen(now)) {
            ticket.expireRegisterWindow();
            throw new CustomException(ErrorCode.TICKET_REGISTER_EXPIRED);
        }
    }

    /** 계정당 ACTIVE 이용권 1개. 기간이 지난 ACTIVE 는 여기서 EXPIRED 로 내리고 통과시킨다. */
    private void rejectIfUserHasActiveTicket(Long userId, LocalDateTime now) {
        Optional<ClassTicket> active = classTicketRepository.findByRegisteredUserIdAndStatus(userId, TicketStatus.ACTIVE);
        if (active.isEmpty()) {
            return;
        }
        if (expireIfNeeded(active.get(), now)) {
            return;
        }
        throw new CustomException(ErrorCode.TICKET_ALREADY_ACTIVE);
    }

    private boolean expireIfNeeded(ClassTicket ticket, LocalDateTime now) {
        if (ticket.getStatus() == TicketStatus.ACTIVE && !ticket.isUsable(now)) {
            ticket.expire();
            return true;
        }
        return ticket.getStatus() == TicketStatus.EXPIRED;
    }
}

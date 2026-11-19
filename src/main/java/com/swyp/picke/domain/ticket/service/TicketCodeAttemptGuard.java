package com.swyp.picke.domain.ticket.service;

import com.swyp.picke.domain.ticket.entity.TicketCodeAttempt;
import com.swyp.picke.domain.ticket.repository.TicketCodeAttemptRepository;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이용권 코드 입력 5회 제한 (기획 결정 2026-11-19). 계정별로 1시간 안에 없는 코드를 5번 넣으면 1시간 동안 막는다.
 * <p>실패 기록은 호출한 쪽이 예외로 롤백돼도 남아야 하므로 별도 트랜잭션(REQUIRES_NEW)으로 저장한다.
 * 맞는 코드를 넣어도 횟수를 초기화하지 않는다. 초기화하면 본인 코드로 횟수를 지우며 계속 추측할 수 있기 때문이다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TicketCodeAttemptGuard {

    public static final int MAX_FAILURES = 5;
    public static final long WINDOW_MINUTES = 60;
    public static final long LOCK_MINUTES = 60;

    private final TicketCodeAttemptRepository attemptRepository;

    /** 잠겨 있으면 429 */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void checkNotLocked(Long userId, LocalDateTime now) {
        attemptRepository.findById(userId)
                .filter(a -> a.isLocked(now))
                .ifPresent(a -> {
                    throw new CustomException(ErrorCode.TICKET_CODE_LOCKED);
                });
    }

    /** 없는 코드 입력 1회 기록. 이번 실패로 잠기면 true */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean recordFailure(Long userId, LocalDateTime now) {
        TicketCodeAttempt attempt = attemptRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> createRow(userId, now));
        boolean locked = attempt.recordFailure(now, MAX_FAILURES, WINDOW_MINUTES, LOCK_MINUTES);
        if (locked) {
            log.warn("이용권 코드 입력 잠금 - userId: {}, lockedUntil: {}", userId, attempt.getLockedUntil());
        }
        return locked;
    }

    /**
     * 같은 계정의 첫 실패가 동시에 들어오면 한쪽은 PK 충돌(DataIntegrityViolationException)로 끝난다.
     * 이 트랜잭션은 그대로 롤백되고, 호출한 쪽이 한 번 더 부르면 이미 만들어진 행을 잠그고 센다.
     */
    private TicketCodeAttempt createRow(Long userId, LocalDateTime now) {
        return attemptRepository.saveAndFlush(new TicketCodeAttempt(userId, now));
    }
}

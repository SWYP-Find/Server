package com.swyp.picke.domain.ticket.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 이용권 코드 입력 실패 횟수 (계정별 1행). 짧은 코드(PKE+6자리)를 추측해 가로채지 못하게,
 * 1시간 안에 없는 코드를 5번 넣으면 1시간 동안 코드 확인·등록을 막는다 (기획 결정 2026-11-19).
 */
@Getter
@Entity
@Table(name = "ticket_code_attempts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TicketCodeAttempt {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "fail_count", nullable = false)
    private int failCount;

    /** 실패 횟수를 세기 시작한 시각. 여기서 1시간이 지나면 횟수를 0부터 다시 센다 */
    @Column(name = "window_started_at")
    private LocalDateTime windowStartedAt;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public TicketCodeAttempt(Long userId, LocalDateTime now) {
        this.userId = userId;
        this.updatedAt = now;
    }

    public boolean isLocked(LocalDateTime now) {
        return lockedUntil != null && now.isBefore(lockedUntil);
    }

    /** 실패 1회를 기록하고, 이번 실패로 잠겼으면 true */
    public boolean recordFailure(LocalDateTime now, int maxFailures, long windowMinutes, long lockMinutes) {
        if (windowStartedAt == null || !now.isBefore(windowStartedAt.plusMinutes(windowMinutes))) {
            windowStartedAt = now;
            failCount = 0;
        }
        failCount++;
        updatedAt = now;
        if (failCount >= maxFailures) {
            lockedUntil = now.plusMinutes(lockMinutes);
            failCount = 0;
            windowStartedAt = null;
            return true;
        }
        return false;
    }
}

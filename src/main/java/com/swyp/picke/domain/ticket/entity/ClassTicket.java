package com.swyp.picke.domain.ticket.entity;

import com.swyp.picke.domain.ticket.enums.TicketCodeStatus;
import com.swyp.picke.domain.ticket.enums.TicketStatus;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 클래스 이용권. 코드 원문은 저장하지 않고 SHA-256 해시만 보관한다.
 * <ul>
 *   <li>registerBy  = 결제 확정일 + 1년 (발급 시 계산)</li>
 *   <li>activatedAt = 등록 시각, expiresAt = activatedAt + 6개월</li>
 *   <li>계정당 ACTIVE 이용권은 1개 (partial unique index, SQL 로 관리)</li>
 * </ul>
 */
@Getter
@Entity
@Table(
        name = "class_tickets",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_class_tickets_funding_order", columnNames = "funding_order_id"),
                @UniqueConstraint(name = "uk_class_tickets_code_hash", columnNames = "code_hash")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClassTicket extends BaseEntity {

    public static final int USAGE_MONTHS = 6;
    public static final int REGISTER_WINDOW_YEARS = 1;
    public static final int READ_GRACE_DAYS = 30;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funding_order_id", nullable = false)
    private FundingOrder fundingOrder;

    @Column(nullable = false)
    private int capacity;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "code_status", nullable = false, length = 20)
    private TicketCodeStatus codeStatus;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "register_by", nullable = false)
    private LocalDateTime registerBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registered_user_id")
    private User registeredUser;

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketStatus status;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Builder
    private ClassTicket(FundingOrder fundingOrder, int capacity, String codeHash, LocalDateTime registerBy) {
        this.fundingOrder = fundingOrder;
        this.capacity = capacity;
        this.codeHash = codeHash;
        this.registerBy = registerBy;
        this.codeStatus = TicketCodeStatus.ISSUED;
        this.status = TicketStatus.UNREGISTERED;
    }

    public static ClassTicket issue(FundingOrder order, String codeHash) {
        return ClassTicket.builder()
                .fundingOrder(order)
                .capacity(order.getRewardTier().getCapacity())
                .codeHash(codeHash)
                .registerBy(order.getPaidAt().plusYears(REGISTER_WINDOW_YEARS))
                .build();
    }

    public void markDelivered(LocalDateTime deliveredAt) {
        this.codeStatus = TicketCodeStatus.DELIVERED;
        this.deliveredAt = deliveredAt;
    }

    public void register(User user, LocalDateTime now) {
        this.registeredUser = user;
        this.activatedAt = now;
        this.expiresAt = now.plusMonths(USAGE_MONTHS);
        this.codeStatus = TicketCodeStatus.CLAIMED;
        this.status = TicketStatus.ACTIVE;
    }

    public void expire() {
        this.status = TicketStatus.EXPIRED;
    }

    public void expireRegisterWindow() {
        this.status = TicketStatus.REGISTER_EXPIRED;
    }

    /** 재발급 시 기존 행을 무효화한다. 새 행은 registerBy 를 그대로 복사해 발급한다. */
    public void revoke(LocalDateTime now) {
        this.codeStatus = TicketCodeStatus.REVOKED;
        this.status = TicketStatus.REVOKED;
        this.revokedAt = now;
    }

    /** 이 계정이 등록한 이용권인지. 본인 코드 재등록은 거절 대신 기존 결과를 돌려주기 위해 쓴다 (PRD §4) */
    public boolean isRegisteredBy(Long userId) {
        return registeredUser != null && registeredUser.getId() != null && registeredUser.getId().equals(userId);
    }

    public boolean isRevoked() {
        return codeStatus == TicketCodeStatus.REVOKED || status == TicketStatus.REVOKED;
    }

    public boolean isRegisterWindowOpen(LocalDateTime now) {
        return !now.isAfter(registerBy);
    }

    public boolean isUsable(LocalDateTime now) {
        return status == TicketStatus.ACTIVE && expiresAt != null && now.isBefore(expiresAt);
    }

    /** 읽기 유예 종료 = expiresAt + 30일 (계산값, 컬럼 아님) */
    public LocalDateTime getReadGraceEndsAt() {
        return expiresAt == null ? null : expiresAt.plusDays(READ_GRACE_DAYS);
    }
}

package com.swyp.picke.domain.ticket.entity;

import com.swyp.picke.domain.ticket.enums.DeliveryChannel;
import com.swyp.picke.domain.ticket.enums.DeliveryStatus;
import com.swyp.picke.domain.ticket.enums.RegisterReminderStage;
import com.swyp.picke.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 미등록 이용권 등록기한 임박 알림 발송 이력 (운영자가 이메일 / SMS / 수동으로 보낸 결과). */
@Getter
@Entity
@Table(name = "ticket_register_reminders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TicketRegisterReminder extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private ClassTicket ticket;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RegisterReminderStage stage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeliveryChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeliveryStatus status;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "attempted_at", nullable = false)
    private LocalDateTime attemptedAt;

    @Builder
    private TicketRegisterReminder(ClassTicket ticket, RegisterReminderStage stage, DeliveryChannel channel,
                                   DeliveryStatus status, String failureReason, LocalDateTime attemptedAt) {
        this.ticket = ticket;
        this.stage = stage;
        this.channel = channel;
        this.status = status;
        this.failureReason = failureReason;
        this.attemptedAt = attemptedAt;
    }
}

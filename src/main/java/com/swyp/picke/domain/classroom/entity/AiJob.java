package com.swyp.picke.domain.classroom.entity;

import com.swyp.picke.domain.classroom.enums.AiJobStatus;
import com.swyp.picke.domain.classroom.enums.AiJobType;
import com.swyp.picke.domain.ticket.entity.ClassTicket;
import com.swyp.picke.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * AI 호출 1건 = 1행. 비용은 ticket 기준으로 합산한다 (PRD §21).
 * idempotencyKey 로 연속 탭 중복 생성을 막는다 (UX04).
 */
@Getter
@Entity
@Table(
        name = "ai_jobs",
        uniqueConstraints = @UniqueConstraint(name = "uk_ai_jobs_idempotency_key", columnNames = "idempotency_key")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiJob extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AiJobType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AiJobStatus status;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id")
    private ClassTicket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id")
    private Classroom classroom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private ClassMember member;

    @Column(length = 50)
    private String model;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(nullable = false)
    private int attempt;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Builder
    private AiJob(AiJobType type, String idempotencyKey, ClassTicket ticket, Classroom classroom, ClassMember member) {
        this.type = type;
        this.idempotencyKey = idempotencyKey;
        this.ticket = ticket;
        this.classroom = classroom;
        this.member = member;
        this.status = AiJobStatus.QUEUED;
        this.attempt = 0;
    }

    public void start(String model, LocalDateTime now) {
        this.status = AiJobStatus.RUNNING;
        this.model = model;
        this.attempt++;
        this.startedAt = now;
    }

    public void succeed(Integer inputTokens, Integer outputTokens, LocalDateTime now) {
        this.status = AiJobStatus.SUCCEEDED;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.finishedAt = now;
    }

    public void fail(String errorCode, LocalDateTime now) {
        this.status = AiJobStatus.FAILED;
        this.errorCode = errorCode;
        this.finishedAt = now;
    }

    public void reject(String errorCode, LocalDateTime now) {
        this.status = AiJobStatus.REJECTED;
        this.errorCode = errorCode;
        this.finishedAt = now;
    }
}

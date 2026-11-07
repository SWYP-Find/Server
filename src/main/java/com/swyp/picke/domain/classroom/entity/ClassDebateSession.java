package com.swyp.picke.domain.classroom.entity;

import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.classroom.enums.ClassDebateStatus;
import com.swyp.picke.domain.user.enums.PhilosopherType;
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
 * 커스텀 배틀의 AI 철학자 토론 세션 (PRD §8 AI-02). 참가자(멤버)·클래스당 1개.
 * <ul>
 *   <li>상대 선택지와 철학자는 시작 시 서버가 정하고 3라운드 동안 바꾸지 않는다 (PRD §11)</li>
 *   <li>round 는 지금 생성 중이거나 반박을 기다리는 라운드(1~3). 단계 이동은 저장된 턴 수와 상태만 믿는다</li>
 * </ul>
 */
@Getter
@Entity
@Table(
        name = "class_debate_sessions",
        uniqueConstraints = @UniqueConstraint(name = "uk_class_debate_sessions_class_member", columnNames = {"class_id", "member_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClassDebateSession extends BaseEntity {

    public static final int ROUNDS = 3;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private Classroom classroom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private ClassMember member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_option_id", nullable = false)
    private BattleOption participantOption;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opponent_option_id", nullable = false)
    private BattleOption opponentOption;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PhilosopherType philosopher;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClassDebateStatus status;

    @Column(name = "current_round", nullable = false)
    private int round;

    @Column(name = "generating_started_at")
    private LocalDateTime generatingStartedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Builder
    private ClassDebateSession(Classroom classroom, ClassMember member, BattleOption participantOption,
                               BattleOption opponentOption, PhilosopherType philosopher, LocalDateTime now) {
        this.classroom = classroom;
        this.member = member;
        this.participantOption = participantOption;
        this.opponentOption = opponentOption;
        this.philosopher = philosopher;
        this.status = ClassDebateStatus.GENERATING;
        this.round = 1;
        this.generatingStartedAt = now;
    }

    /** 오래 멈춘 GENERATING 은 실패로 보고 재시도를 허용한다 */
    public boolean isRetryable(LocalDateTime staleBefore) {
        return status == ClassDebateStatus.FAILED
                || (status == ClassDebateStatus.GENERATING && generatingStartedAt != null && generatingStartedAt.isBefore(staleBefore));
    }

    public void startGenerating(int round, LocalDateTime now) {
        this.round = round;
        this.status = ClassDebateStatus.GENERATING;
        this.generatingStartedAt = now;
    }

    public void waitForUser() {
        this.status = ClassDebateStatus.WAITING_USER;
    }

    public void fail() {
        this.status = ClassDebateStatus.FAILED;
    }

    public void complete(LocalDateTime now) {
        this.status = ClassDebateStatus.COMPLETED;
        this.completedAt = now;
    }
}

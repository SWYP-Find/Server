package com.swyp.picke.domain.classroom.entity;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.classroom.enums.ClassBattleType;
import com.swyp.picke.domain.classroom.enums.ClassStatus;
import com.swyp.picke.domain.ticket.entity.ClassTicket;
import com.swyp.picke.domain.user.entity.User;
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
 * 클래스 (테이블명 classes). 자바 예약어 때문에 엔티티명은 Classroom 을 쓴다.
 * <ul>
 *   <li>capacity 는 생성 시점 이용권 capacity 복사 (운영자 참여 시 운영자 1석 포함)</li>
 *   <li>deadline null = 마감 없음. 설정 시 "현재 시각 이후 ~ 이용권 종료일" 범위만 허용</li>
 *   <li>실제 마감 시각 = min(deadline, ticket.expiresAt) → {@link #getEffectiveCloseAt()}</li>
 *   <li>개설 후 수정 가능 항목은 name, deadline 연장뿐</li>
 * </ul>
 */
@Getter
@Entity
@Table(
        name = "classes",
        uniqueConstraints = @UniqueConstraint(name = "uk_classes_invite_code", columnNames = "invite_code")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Classroom extends BaseEntity {

    public static final int NAME_MAX_LENGTH = 20;
    public static final int INVITE_CODE_LENGTH = 6;

    @Column(nullable = false, length = NAME_MAX_LENGTH)
    private String name;

    @Column(name = "invite_code", nullable = false, length = INVITE_CODE_LENGTH)
    private String inviteCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private ClassTicket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "battle_id", nullable = false)
    private Battle battle;

    @Enumerated(EnumType.STRING)
    @Column(name = "battle_type", nullable = false, length = 10)
    private ClassBattleType battleType;

    @Column(nullable = false)
    private int capacity;

    private LocalDateTime deadline;

    @Column(name = "comment_required", nullable = false)
    private boolean commentRequired;

    @Column(name = "owner_participates", nullable = false)
    private boolean ownerParticipates;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClassStatus status;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    private Classroom(String name, String inviteCode, User owner, ClassTicket ticket, Battle battle,
                      ClassBattleType battleType, int capacity, LocalDateTime deadline,
                      boolean commentRequired, boolean ownerParticipates) {
        this.name = name;
        this.inviteCode = inviteCode;
        this.owner = owner;
        this.ticket = ticket;
        this.battle = battle;
        this.battleType = battleType;
        this.capacity = capacity;
        this.deadline = deadline;
        this.commentRequired = commentRequired;
        this.ownerParticipates = ownerParticipates;
        this.status = ClassStatus.ACTIVE;
    }

    public void rename(String name) {
        this.name = name;
    }

    public void extendDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }

    public void delete(LocalDateTime now) {
        this.status = ClassStatus.DELETED;
        this.deletedAt = now;
    }

    public LocalDateTime getEffectiveCloseAt() {
        LocalDateTime ticketEnd = ticket.getExpiresAt();
        if (deadline == null) {
            return ticketEnd;
        }
        if (ticketEnd == null) {
            return deadline;
        }
        return deadline.isBefore(ticketEnd) ? deadline : ticketEnd;
    }

    public boolean isClosed(LocalDateTime now) {
        LocalDateTime closeAt = getEffectiveCloseAt();
        return closeAt != null && !now.isBefore(closeAt);
    }
}

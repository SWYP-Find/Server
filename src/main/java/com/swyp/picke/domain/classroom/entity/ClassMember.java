package com.swyp.picke.domain.classroom.entity;

import com.swyp.picke.domain.classroom.enums.ClassMemberRole;
import com.swyp.picke.domain.classroom.enums.ClassMemberStatus;
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
 * 클래스 멤버. 운영자는 ownerParticipates = true 일 때만 행이 생긴다.
 * <ul>
 *   <li>displayName : 클래스 안에서만 보이는 별칭. ACTIVE 멤버끼리 중복 불가 (partial unique index, SQL 로 관리)</li>
 *   <li>seatHeld : 좌석 소비 여부. 사전투표 이후 활동이 있으면 나가도 true 유지 → 정원 판정 = COUNT(seatHeld)</li>
 *   <li>참여 단계는 UserBattle.step (class_id 스코프) 로 추적</li>
 * </ul>
 */
@Getter
@Entity
@Table(
        name = "class_members",
        uniqueConstraints = @UniqueConstraint(name = "uk_class_members_class_user", columnNames = {"class_id", "user_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClassMember extends BaseEntity {

    public static final int DISPLAY_NAME_MAX_LENGTH = 20;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private Classroom classroom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "display_name", nullable = false, length = DISPLAY_NAME_MAX_LENGTH)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ClassMemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ClassMemberStatus status;

    @Column(name = "seat_held", nullable = false)
    private boolean seatHeld;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @Builder
    private ClassMember(Classroom classroom, User user, String displayName, ClassMemberRole role, LocalDateTime joinedAt) {
        this.classroom = classroom;
        this.user = user;
        this.displayName = displayName;
        this.role = role;
        this.joinedAt = joinedAt;
        this.status = ClassMemberStatus.ACTIVE;
        this.seatHeld = true;
    }

    public void changeDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /** 사전투표 전에 나가면 좌석을 반환한다. */
    public void leave(LocalDateTime now, boolean hadActivity) {
        this.status = ClassMemberStatus.LEFT;
        this.leftAt = now;
        this.seatHeld = hadActivity;
    }

    /** 운영자가 내보냄. 되돌릴 수 없다. */
    public void remove(LocalDateTime now, boolean hadActivity) {
        this.status = ClassMemberStatus.REMOVED;
        this.leftAt = now;
        this.seatHeld = hadActivity;
    }

    public boolean isActive() {
        return status == ClassMemberStatus.ACTIVE;
    }
}

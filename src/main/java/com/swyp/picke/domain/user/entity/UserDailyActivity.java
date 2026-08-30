package com.swyp.picke.domain.user.entity;

import com.swyp.picke.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 어드민 대시보드 DAU/MAU 집계를 위한 유저-날짜별 활동 기록.
 * {@code active}/{@code logged_in}은 JwtFilter/AuthService에서 UPSERT로 기록되며,
 * 이 엔티티는 조회 전용으로만 사용한다.
 */
@Getter
@Entity
@Table(
        name = "user_daily_activities",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "activity_date"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserDailyActivity extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;

    @Column(name = "logged_in", nullable = false)
    private boolean loggedIn;

    @Column(name = "active", nullable = false)
    private boolean active;
}

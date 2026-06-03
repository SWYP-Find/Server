package com.swyp.picke.domain.attendance.entity;

import com.swyp.picke.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "attendance_streaks")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AttendanceStreak {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "current_streak", nullable = false)
    private int currentStreak;

    @Column(name = "max_streak", nullable = false)
    private int maxStreak;

    @Column(name = "streak_week_start", nullable = false)
    private LocalDate streakWeekStart;

    @Column(name = "is_streak_achieved", nullable = false)
    private boolean isStreakAchieved;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private AttendanceStreak(User user, int currentStreak, int maxStreak,
                              LocalDate streakWeekStart, boolean isStreakAchieved) {
        this.user = user;
        this.currentStreak = currentStreak;
        this.maxStreak = maxStreak;
        this.streakWeekStart = streakWeekStart;
        this.isStreakAchieved = isStreakAchieved;
        this.updatedAt = LocalDateTime.now();
    }

    public void attend(LocalDate today) {
        this.currentStreak++;
        if (this.currentStreak > this.maxStreak) {
            this.maxStreak = this.currentStreak;
        }
        if (this.currentStreak >= 7) {
            this.isStreakAchieved = true;
        }
        this.updatedAt = LocalDateTime.now();
    }

    public void resetStreak(LocalDate weekStart) {
        this.currentStreak = 1;
        this.streakWeekStart = weekStart;
        this.isStreakAchieved = false;
        this.updatedAt = LocalDateTime.now();
    }

    public void initWeek(LocalDate weekStart) {
        this.currentStreak = 0;
        this.streakWeekStart = weekStart;
        this.isStreakAchieved = false;
        this.updatedAt = LocalDateTime.now();
    }
}

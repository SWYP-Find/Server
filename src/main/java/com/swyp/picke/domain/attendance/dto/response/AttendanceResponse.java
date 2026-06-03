package com.swyp.picke.domain.attendance.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.swyp.picke.domain.attendance.enums.AttendanceStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class AttendanceResponse {

    public record CheckResponse(
            @JsonProperty("user_tag") String userTag,
            @JsonProperty("attended_at") LocalDateTime attendedAt,
            @JsonProperty("points_earned") int pointsEarned,
            @JsonProperty("streak_bonus_earned") boolean streakBonusEarned,
            @JsonProperty("streak_bonus_points") int streakBonusPoints,
            @JsonProperty("consecutive_days") int consecutiveDays,
            @JsonProperty("total_points") int totalPoints
    ) {}

    public record WeeklyResponse(
            @JsonProperty("user_tag") String userTag,
            @JsonProperty("week_start_date") LocalDate weekStartDate,
            @JsonProperty("consecutive_days") int consecutiveDays,
            @JsonProperty("is_streak_achieved") boolean isStreakAchieved,
            @JsonProperty("weekly_attendance") List<DayAttendance> weeklyAttendance,
            @JsonProperty("streak_reward_points") int streakRewardPoints
    ) {
        public record DayAttendance(
                String day,
                LocalDate date,
                AttendanceStatus status,
                int points
        ) {}
    }

    public record SummaryResponse(
            @JsonProperty("user_tag") String userTag,
            @JsonProperty("total_attended_days") long totalAttendedDays,
            @JsonProperty("current_consecutive_days") int currentConsecutiveDays,
            @JsonProperty("max_consecutive_days") int maxConsecutiveDays,
            @JsonProperty("total_points_from_attendance") int totalPointsFromAttendance,
            @JsonProperty("last_attended_at") LocalDateTime lastAttendedAt
    ) {}
}

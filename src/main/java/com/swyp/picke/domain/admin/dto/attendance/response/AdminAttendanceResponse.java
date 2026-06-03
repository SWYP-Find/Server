package com.swyp.picke.domain.admin.dto.attendance.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class AdminAttendanceResponse {

    // GET /api/v1/admin/attendance/stats
    public record StatsResponse(
            LocalDate from,
            LocalDate to,
            @JsonProperty("total_users") long totalUsers,
            @JsonProperty("daily_stats") List<DailyStat> dailyStats
    ) {
        public record DailyStat(
                LocalDate date,
                @JsonProperty("attended_count") long attendedCount,
                @JsonProperty("attendance_rate") double attendanceRate
        ) {}
    }

    // GET /api/v1/admin/attendance/users/{user_tag}
    public record UserAttendanceResponse(
            @JsonProperty("user_tag") String userTag,
            AttendanceSummary summary,
            @JsonProperty("total_count") long totalCount,
            int page,
            int size,
            List<AttendanceItem> items
    ) {
        public record AttendanceSummary(
                @JsonProperty("total_attended_days") long totalAttendedDays,
                @JsonProperty("current_consecutive_days") int currentConsecutiveDays,
                @JsonProperty("max_consecutive_days") int maxConsecutiveDays,
                @JsonProperty("total_points_from_attendance") int totalPointsFromAttendance,
                @JsonProperty("last_attended_at") LocalDateTime lastAttendedAt
        ) {}

        public record AttendanceItem(
                @JsonProperty("attended_at") LocalDateTime attendedAt,
                @JsonProperty("consecutive_days") int consecutiveDays,
                @JsonProperty("points_earned") int pointsEarned,
                @JsonProperty("streak_bonus_earned") boolean streakBonusEarned
        ) {}
    }
}

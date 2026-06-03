package com.swyp.picke.domain.attendance.entity;

import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Entity
@Table(
        name = "attendance_records",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_attendance_user_date",
                        columnNames = {"user_id", "attended_date"}
                )
        },
        indexes = {
                @Index(name = "idx_attendance_record_user_id", columnList = "user_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AttendanceRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "attended_date", nullable = false)
    private LocalDate attendedDate;

    @Column(name = "points_earned", nullable = false)
    private int pointsEarned;

    @Builder
    private AttendanceRecord(User user, LocalDate attendedDate, int pointsEarned) {
        this.user = user;
        this.attendedDate = attendedDate;
        this.pointsEarned = pointsEarned;
    }
}

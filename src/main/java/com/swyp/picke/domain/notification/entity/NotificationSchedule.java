package com.swyp.picke.domain.notification.entity;

import com.swyp.picke.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "notification_schedules")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationSchedule extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 150)
    private String subtitle;

    @Column(name = "send_time", nullable = false)
    private LocalTime sendTime;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "last_sent_date")
    private LocalDate lastSentDate;

    @Builder
    private NotificationSchedule(String title, String subtitle, LocalTime sendTime, boolean enabled) {
        this.title = title;
        this.subtitle = subtitle;
        this.sendTime = sendTime;
        this.enabled = enabled;
    }

    public void update(String title, String subtitle, LocalTime sendTime, boolean enabled) {
        this.title = title;
        this.subtitle = subtitle;
        this.sendTime = sendTime;
        this.enabled = enabled;
    }

    public void changeEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isDue(LocalTime now, LocalDate today) {
        return enabled
                && sendTime.getHour() == now.getHour()
                && sendTime.getMinute() == now.getMinute()
                && !today.equals(lastSentDate);
    }

    public void markSent(LocalDate today) {
        this.lastSentDate = today;
    }
}

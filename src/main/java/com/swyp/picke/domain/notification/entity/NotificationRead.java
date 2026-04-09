package com.swyp.picke.domain.notification.entity;

import com.swyp.picke.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "notification_reads",
        uniqueConstraints = @UniqueConstraint(columnNames = {"notification_id", "user_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationRead extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Builder
    private NotificationRead(Notification notification, Long userId) {
        this.notification = notification;
        this.userId = userId;
    }
}

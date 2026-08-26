package com.swyp.picke.domain.notification.entity;

import com.swyp.picke.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "notification_delivery_results")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationDeliveryResult extends BaseEntity {

    @Column(name = "notification_id", nullable = false, unique = true)
    private Long notificationId;

    @Column(name = "target_count", nullable = false)
    private int targetCount;

    @Column(name = "success_count", nullable = false)
    private int successCount;

    @Column(name = "failure_count", nullable = false)
    private int failureCount;

    @Builder
    private NotificationDeliveryResult(Long notificationId, int targetCount) {
        this.notificationId = notificationId;
        this.targetCount = targetCount;
        this.successCount = 0;
        this.failureCount = 0;
    }

    public boolean isPending() {
        return successCount + failureCount < targetCount;
    }
}

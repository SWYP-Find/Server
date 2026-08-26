package com.swyp.picke.domain.notification.entity;

import com.swyp.picke.domain.notification.enums.NotificationCategory;
import com.swyp.picke.domain.notification.enums.NotificationDetailCode;
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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "notifications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "detail_code", nullable = false, length = 30)
    private NotificationDetailCode detailCode;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "perspective_id")
    private Long perspectiveId;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    private Notification(User user, NotificationCategory category, NotificationDetailCode detailCode,
                         String title, String body, Long referenceId, Long perspectiveId) {
        this.user = user;
        this.category = category;
        this.detailCode = detailCode;
        this.title = title;
        this.body = body;
        this.referenceId = referenceId;
        this.perspectiveId = perspectiveId;
        this.read = false;
    }

    public void updateContent(String title, String body) {
        this.title = title;
        this.body = body;
    }

    public void markAsRead() {
        if (!this.read) {
            this.read = true;
            this.readAt = LocalDateTime.now();
        }
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }
}

package com.swyp.picke.domain.notification.entity;

import com.swyp.picke.domain.notification.enums.DevicePlatform;
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
@Table(name = "user_devices")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserDevice extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "fcm_token", nullable = false, unique = true, length = 255)
    private String fcmToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DevicePlatform platform;

    @Column(name = "last_used_at", nullable = false)
    private LocalDateTime lastUsedAt;

    @Builder
    private UserDevice(User user, String fcmToken, DevicePlatform platform) {
        this.user = user;
        this.fcmToken = fcmToken;
        this.platform = platform;
        this.lastUsedAt = LocalDateTime.now();
    }

    public void reassign(User user, DevicePlatform platform) {
        this.user = user;
        this.platform = platform;
        this.lastUsedAt = LocalDateTime.now();
    }
}

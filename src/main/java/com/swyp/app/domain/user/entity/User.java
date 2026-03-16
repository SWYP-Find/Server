package com.swyp.app.domain.user.entity;

import com.swyp.app.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_tag", nullable = false, unique = true, length = 30)
    private String userTag;

    @Column(length = 50)
    private String nickname;

    @Column(name = "character_url", columnDefinition = "TEXT")
    private String characterUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "onboarding_completed", nullable = false)
    private boolean onboardingCompleted;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    private User(String userTag, String nickname, String characterUrl, UserRole role, UserStatus status, boolean onboardingCompleted) {
        this.userTag = userTag;
        this.nickname = nickname;
        this.characterUrl = characterUrl;
        this.role = role;
        this.status = status;
        this.onboardingCompleted = onboardingCompleted;
    }

    public void completeOnboarding() {
        this.status = UserStatus.ACTIVE;
        this.onboardingCompleted = true;
    }

    public void delete() {
        this.status = UserStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
    }
}

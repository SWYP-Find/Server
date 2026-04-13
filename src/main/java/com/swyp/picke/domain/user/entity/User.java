package com.swyp.picke.domain.user.entity;

import com.swyp.picke.domain.user.enums.UserRole;
import com.swyp.picke.domain.user.enums.UserStatus;
import com.swyp.picke.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private int credit = 0;

    @Builder
    private User(String userTag, String nickname, String characterUrl, UserRole role, UserStatus status) {
        this.userTag = userTag;
        this.nickname = nickname;
        this.characterUrl = characterUrl;
        this.role = role;
        this.status = status;
        this.credit = 0;
    }

    public void delete() {
        this.status = UserStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * 테스트나 메모리 상 도메인 계산에서만 사용하는 보조 메서드.
     * 실제 영속 잔액 반영은 CreditService 가 원자 UPDATE 쿼리로 처리한다.
     */
    public void addCredit(int amount) {
        this.credit += amount;
    }
}

package com.swyp.app.domain.user.entity;

import com.swyp.app.global.common.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Entity
@Table(name = "user_profiles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String nickname;

    private CharacterType characterType;

    @Enumerated(EnumType.STRING)
    private PhilosopherType philosopherType;

    private BigDecimal mannerTemperature;

    @Builder
    private UserProfile(User user, String nickname, CharacterType characterType, BigDecimal mannerTemperature) {
        this.user = user;
        this.nickname = nickname;
        this.characterType = characterType;
        this.mannerTemperature = mannerTemperature;
    }

    public void update(String nickname, CharacterType characterType) {
        if (nickname != null && !nickname.isBlank()) {
            this.nickname = nickname;
        }
        if (characterType != null) {
            this.characterType = characterType;
        }
    }

    public void updatePhilosopherType(PhilosopherType philosopherType) {
        this.philosopherType = philosopherType;
    }
}

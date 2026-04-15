package com.swyp.picke.domain.user.entity;

import com.swyp.picke.domain.user.enums.CharacterType;
import com.swyp.picke.domain.user.enums.PhilosopherType;
import com.swyp.picke.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Objects;

@Getter
@Entity
@Table(name = "user_profiles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String nickname;

    @Column(nullable = false)
    private CharacterType characterType;

    @Enumerated(EnumType.STRING)
    private PhilosopherType philosopherType;

    @Column(name = "recap_share_key", unique = true, length = 36)
    private String recapShareKey;

    private BigDecimal mannerTemperature;

    @Builder
    private UserProfile(User user, String nickname, CharacterType characterType, String recapShareKey, BigDecimal mannerTemperature) {
        this.user = user;
        this.nickname = nickname;
        this.characterType = Objects.requireNonNull(characterType, "characterType must not be null");
        this.recapShareKey = recapShareKey;
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

    public void updateRecapShareKey(String recapShareKey) {
        this.recapShareKey = recapShareKey;
    }
}

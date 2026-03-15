package com.swyp.app.domain.battle.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "battle_options")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BattleOption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "battle_id", nullable = false)
    private Battle battle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    private BattleOptionLabel label;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 255)
    private String stance;

    @Column(length = 100)
    private String representative;

    @Column(columnDefinition = "TEXT")
    private String quote;

    @Column(columnDefinition = "jsonb")
    private String keywords;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Builder
    private BattleOption(Battle battle, BattleOptionLabel label, String title, String stance,
                         String representative, String quote, String keywords, String imageUrl) {
        this.battle = battle;
        this.label = label;
        this.title = title;
        this.stance = stance;
        this.representative = representative;
        this.quote = quote;
        this.keywords = keywords;
        this.imageUrl = imageUrl;
    }
}

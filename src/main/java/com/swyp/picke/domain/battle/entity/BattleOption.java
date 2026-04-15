package com.swyp.picke.domain.battle.entity;

import com.swyp.picke.domain.battle.enums.BattleOptionLabel;
import com.swyp.picke.global.common.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "battle_options")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BattleOption extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "battle_id", nullable = false)
    private Battle battle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private BattleOptionLabel label;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 255)
    private String stance;

    @Column(length = 100)
    private String representative;

    @Column(name = "vote_count")
    private Long voteCount = 0L;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "display_order")
    private Integer displayOrder;

    @OneToMany(mappedBy = "battleOption", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<BattleOptionTag> tags = new ArrayList<>();

    @Builder
    public BattleOption(
            Battle battle,
            BattleOptionLabel label,
            String title,
            String stance,
            String representative,
            String imageUrl,
            Integer displayOrder
    ) {
        this.battle = battle;
        this.label = label;
        this.title = title;
        this.stance = stance;
        this.representative = representative;
        this.imageUrl = imageUrl;
        this.displayOrder = displayOrder;
        this.voteCount = 0L;
    }

    public void increaseVoteCount() {
        this.voteCount = (this.voteCount == null ? 0L : this.voteCount) + 1;
    }

    public void decreaseVoteCount() {
        if (this.voteCount != null && this.voteCount > 0) {
            this.voteCount--;
        }
    }

    public void update(String title, String stance, String representative, String imageUrl) {
        if (title != null) {
            this.title = title;
        }
        if (stance != null) {
            this.stance = stance;
        }
        if (representative != null) {
            this.representative = representative;
        }
        if (imageUrl != null) {
            this.imageUrl = imageUrl;
        }
        if (displayOrder != null) {
            this.displayOrder = displayOrder;
        }
    }
}

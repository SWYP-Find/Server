package com.swyp.app.domain.battle.entity;

import com.swyp.app.domain.tag.entity.Tag;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(
        name = "battle_option_tags",
        uniqueConstraints = @UniqueConstraint(columnNames = {"battle_option_id", "tag_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BattleOptionTag {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "battle_option_id", nullable = false)
    private BattleOption battleOption;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    @Builder
    private BattleOptionTag(BattleOption battleOption, Tag tag) {
        this.battleOption = battleOption;
        this.tag = tag;
    }
}
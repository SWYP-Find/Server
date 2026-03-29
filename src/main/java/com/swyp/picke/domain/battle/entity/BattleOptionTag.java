package com.swyp.picke.domain.battle.entity;

import com.swyp.picke.domain.tag.entity.Tag;
import com.swyp.picke.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "battle_option_tags",
        uniqueConstraints = @UniqueConstraint(columnNames = {"battle_option_id", "tag_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BattleOptionTag extends BaseEntity {

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
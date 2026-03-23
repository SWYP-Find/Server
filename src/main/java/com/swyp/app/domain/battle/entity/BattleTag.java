package com.swyp.app.domain.battle.entity;

import com.swyp.app.domain.tag.entity.Tag;
import com.swyp.app.global.common.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "battle_tags",
        uniqueConstraints = @UniqueConstraint(columnNames = {"battle_id", "tag_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BattleTag extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "battle_id", nullable = false)
    private Battle battle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    @Builder
    private BattleTag(Battle battle, Tag tag) {
        this.battle = battle;
        this.tag = tag;
    }
}

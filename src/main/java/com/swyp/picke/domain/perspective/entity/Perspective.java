package com.swyp.picke.domain.perspective.entity;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.perspective.enums.PerspectiveStatus;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "perspectives",
        uniqueConstraints = @UniqueConstraint(columnNames = {"battle_id", "user_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Perspective extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "battle_id", nullable = false)
    private Battle battle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id", nullable = false)
    private BattleOption option;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "like_count", nullable = false)
    private int likeCount = 0;

    @Column(name = "comment_count", nullable = false)
    private int commentCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PerspectiveStatus status;

    @Builder
    private Perspective(Battle battle, User user, BattleOption option, String content) {
        this.battle = battle;
        this.user = user;
        this.option = option;
        this.content = content;
        this.likeCount = 0;
        this.commentCount = 0;
        this.status = PerspectiveStatus.PENDING;
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public void updateStatus(PerspectiveStatus status) {
        this.status = status;
    }

    public void publish() {
        this.status = PerspectiveStatus.PUBLISHED;
    }

    public void reject() {
        this.status = PerspectiveStatus.REJECTED;
    }

    public void hide() {
        this.status = PerspectiveStatus.HIDDEN;
    }

    public void incrementLikeCount() {
        this.likeCount++;
    }

    public void decrementLikeCount() {
        if (this.likeCount > 0) this.likeCount--;
    }

    public void incrementCommentCount() {
        this.commentCount++;
    }

    public void decrementCommentCount() {
        if (this.commentCount > 0) this.commentCount--;
    }
}

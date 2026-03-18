package com.swyp.app.domain.perspective.entity;

import com.swyp.app.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(
        name = "perspectives",
        uniqueConstraints = @UniqueConstraint(columnNames = {"battle_id", "user_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Perspective extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // TODO: Battle 엔티티 병합 후 @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "battle_id") 로 교체
    @Column(name = "battle_id", nullable = false)
    private UUID battleId;

    // TODO: User 엔티티 병합 후 @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id") 로 교체
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // TODO: BattleOption 엔티티 병합 후 @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "option_id") 로 교체
    @Column(name = "option_id", nullable = false)
    private UUID optionId;

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
    private Perspective(UUID battleId, Long userId, UUID optionId, String content) {
        this.battleId = battleId;
        this.userId = userId;
        this.optionId = optionId;
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

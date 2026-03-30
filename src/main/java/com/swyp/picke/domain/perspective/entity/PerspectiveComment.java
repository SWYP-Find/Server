package com.swyp.picke.domain.perspective.entity;

import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "perspective_comments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PerspectiveComment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "perspective_id", nullable = false)
    private Perspective perspective;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "like_count", nullable = false)
    private int likeCount = 0;

    @Column(nullable = false)
    private boolean hidden = false;

    @Builder
    private PerspectiveComment(Perspective perspective, User user, String content) {
        this.perspective = perspective;
        this.user = user;
        this.content = content;
        this.likeCount = 0;
        this.hidden = false;
    }

    public void hide() {
        this.hidden = true;
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public void incrementLikeCount() {
        this.likeCount++;
    }

    public void decrementLikeCount() {
        if (this.likeCount > 0) this.likeCount--;
    }
}

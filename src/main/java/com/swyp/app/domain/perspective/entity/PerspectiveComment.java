package com.swyp.app.domain.perspective.entity;

import com.swyp.app.global.common.BaseEntity;
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

    // TODO: User 엔티티 병합 후 @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id") 로 교체
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Builder
    private PerspectiveComment(Perspective perspective, Long userId, String content) {
        this.perspective = perspective;
        this.userId = userId;
        this.content = content;
    }

    public void updateContent(String content) {
        this.content = content;
    }
}

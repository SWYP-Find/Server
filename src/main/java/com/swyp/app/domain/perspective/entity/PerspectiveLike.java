package com.swyp.app.domain.perspective.entity;

import com.swyp.app.domain.user.entity.User;
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
        name = "perspective_likes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"perspective_id", "user_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PerspectiveLike extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "perspective_id", nullable = false)
    private Perspective perspective;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder
    private PerspectiveLike(Perspective perspective, User user) {
        this.perspective = perspective;
        this.user = user;
    }
}

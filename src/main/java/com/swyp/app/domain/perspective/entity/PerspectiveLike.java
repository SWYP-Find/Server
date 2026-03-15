package com.swyp.app.domain.perspective.entity;

import com.swyp.app.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
        name = "perspective_likes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"perspective_id", "user_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PerspectiveLike extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "perspective_id", nullable = false)
    private Perspective perspective;

    // TODO: User 엔티티 병합 후 @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id") 로 교체
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Builder
    private PerspectiveLike(Perspective perspective, Long userId) {
        this.perspective = perspective;
        this.userId = userId;
    }
}

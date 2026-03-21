package com.swyp.app.domain.user.entity;

import com.swyp.app.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "notices")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NoticeType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(name = "body_preview", length = 300)
    private String bodyPreview;

    @Column(name = "is_pinned", nullable = false)
    private boolean isPinned;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Builder
    private Notice(NoticeType type, String title, String body, String bodyPreview,
                   boolean isPinned, LocalDateTime publishedAt) {
        this.type = type;
        this.title = title;
        this.body = body;
        this.bodyPreview = bodyPreview;
        this.isPinned = isPinned;
        this.publishedAt = publishedAt;
    }
}

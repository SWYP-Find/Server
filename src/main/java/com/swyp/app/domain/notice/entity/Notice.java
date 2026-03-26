package com.swyp.app.domain.notice.entity;

import com.swyp.app.domain.notice.enums.NoticePlacement;
import com.swyp.app.domain.notice.enums.NoticeType;
import com.swyp.app.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "notice_type", nullable = false, length = 30)
    private NoticeType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NoticePlacement placement;

    @Column(name = "is_pinned", nullable = false)
    private boolean pinned;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "ends_at")
    private LocalDateTime endsAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    private Notice(String title, String body, NoticeType type, NoticePlacement placement, boolean pinned,
                   LocalDateTime startsAt, LocalDateTime endsAt) {
        this.title = title;
        this.body = body;
        this.type = type;
        this.placement = placement;
        this.pinned = pinned;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }
}

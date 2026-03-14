package com.swyp.app.domain.battle.entity;

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

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Entity
@Table(name = "battles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Battle extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 500)
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BattleStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "creator_type", nullable = false, length = 10)
    private BattleCreatorType creatorType;

    // TODO: User 엔티티 병합 후 @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "creator_id") 로 교체
    @Column(name = "creator_id")
    private Long creatorId;

    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    @Builder
    private Battle(String title, String summary, String description, String thumbnailUrl,
                   LocalDate targetDate, BattleStatus status, BattleCreatorType creatorType,
                   Long creatorId, String rejectReason) {
        this.title = title;
        this.summary = summary;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.targetDate = targetDate;
        this.status = status;
        this.creatorType = creatorType;
        this.creatorId = creatorId;
        this.rejectReason = rejectReason;
    }
}

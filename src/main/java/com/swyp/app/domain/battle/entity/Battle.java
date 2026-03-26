package com.swyp.app.domain.battle.entity;

import com.swyp.app.domain.battle.enums.BattleCreatorType;
import com.swyp.app.domain.battle.enums.BattleStatus;
import com.swyp.app.domain.battle.enums.BattleType;
import com.swyp.app.domain.user.entity.User;
import com.swyp.app.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "battles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Battle extends BaseEntity {

    @Column(nullable = false)
    private String title;

    private String summary;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BattleType type;

    @Column(name = "title_prefix")
    private String titlePrefix;

    @Column(name = "title_suffix")
    private String titleSuffix;

    @Column(name = "item_a")
    private String itemA;

    @Column(name = "item_a_desc")
    private String itemADesc;

    @Column(name = "item_b")
    private String itemB;

    @Column(name = "item_b_desc")
    private String itemBDesc;

    @Column(name = "view_count")
    private Integer viewCount = 0;

    @Column(name = "total_participants")
    private Long totalParticipantsCount = 0L;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Column(name = "audio_duration")
    private Integer audioDuration;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BattleStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "creator_type", nullable = false, length = 10)
    private BattleCreatorType creatorType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    private User creator;

    // 홈 화면 5단 기획을 위한 필드들

    @Column(name = "is_editor_pick")
    private Boolean isEditorPick = false;

    @Column(name = "comment_count")
    private Long commentCount = 0L;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public Battle(String title, String summary, String description, String thumbnailUrl,
                  BattleType type, String titlePrefix, String titleSuffix,
                  String itemA, String itemADesc, String itemB, String itemBDesc, // 🚀 추가
                  LocalDate targetDate, Integer audioDuration, BattleStatus status,
                  BattleCreatorType creatorType, User creator) {
        this.title = title;
        this.summary = summary;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.type = type;
        this.titlePrefix = titlePrefix;
        this.titleSuffix = titleSuffix;
        this.itemA = itemA;
        this.itemADesc = itemADesc;
        this.itemB = itemB;
        this.itemBDesc = itemBDesc;
        this.targetDate = targetDate;
        this.audioDuration = audioDuration;
        this.status = status;
        this.creatorType = creatorType;
        this.creator = creator;
        this.viewCount = 0;
        this.totalParticipantsCount = 0L;
        this.isEditorPick = false;
        this.commentCount = 0L;
        this.deletedAt = null;
    }

    public void update(String title, String titlePrefix, String titleSuffix,
                       String itemA, String itemADesc, String itemB, String itemBDesc, // 🚀 퀴즈 전용 필드 4개 추가
                       String summary, String description,
                       String thumbnailUrl, LocalDate targetDate,
                       Integer audioDuration, BattleStatus status) {
        if (title != null) this.title = title;
        if (titlePrefix != null) this.titlePrefix = titlePrefix;
        if (titleSuffix != null) this.titleSuffix = titleSuffix;

        if (itemA != null) this.itemA = itemA;
        if (itemADesc != null) this.itemADesc = itemADesc;
        if (itemB != null) this.itemB = itemB;
        if (itemBDesc != null) this.itemBDesc = itemBDesc;

        if (summary != null) this.summary = summary;
        if (description != null) this.description = description;
        if (thumbnailUrl != null) this.thumbnailUrl = thumbnailUrl;
        if (targetDate != null) this.targetDate = targetDate;
        if (audioDuration != null) this.audioDuration = audioDuration;
        if (status != null) this.status = status;
    }

    public void delete() {
        this.status = BattleStatus.ARCHIVED;
        this.deletedAt = LocalDateTime.now();
    }

    public void increaseViewCount() {
        this.viewCount = (this.viewCount == null ? 0 : this.viewCount) + 1;
    }

    public void addParticipant() {
        this.totalParticipantsCount = (this.totalParticipantsCount == null ? 0L : this.totalParticipantsCount) + 1;
    }
}
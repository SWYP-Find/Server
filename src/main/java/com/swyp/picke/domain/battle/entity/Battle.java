package com.swyp.picke.domain.battle.entity;

import com.swyp.picke.domain.battle.enums.BattleCreatorType;
import com.swyp.picke.domain.battle.enums.BattleStatus;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.global.common.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @OneToMany(mappedBy = "battle", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<BattleOption> options = new ArrayList<>();

    @Column(name = "is_editor_pick")
    private Boolean isEditorPick = false;

    @Column(name = "comment_count")
    private Long commentCount = 0L;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public Battle(
            String title,
            String summary,
            String description,
            String thumbnailUrl,
            LocalDate targetDate,
            Integer audioDuration,
            BattleStatus status,
            BattleCreatorType creatorType,
            User creator
    ) {
        this.title = title;
        this.summary = summary;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
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

    public void update(
            String title,
            String summary,
            String description,
            String thumbnailUrl,
            LocalDate targetDate,
            Integer audioDuration,
            BattleStatus status
    ) {
        if (title != null) {
            this.title = title;
        }
        if (summary != null) {
            this.summary = summary;
        }
        if (description != null) {
            this.description = description;
        }
        if (thumbnailUrl != null) {
            this.thumbnailUrl = thumbnailUrl;
        }
        if (targetDate != null) {
            this.targetDate = targetDate;
        }
        if (audioDuration != null) {
            this.audioDuration = audioDuration;
        }
        if (status != null) {
            this.status = status;
        }
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

    public void updateAudioDuration(Integer audioDuration) {
        this.audioDuration = audioDuration;
    }

    public void updateTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }

}

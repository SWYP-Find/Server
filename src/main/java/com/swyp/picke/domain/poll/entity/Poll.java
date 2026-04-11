package com.swyp.picke.domain.poll.entity;

import com.swyp.picke.domain.poll.enums.PollStatus;
import com.swyp.picke.global.common.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "poll_contents")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Poll extends BaseEntity {

    @Column(name = "title_prefix", nullable = false, length = 200)
    private String titlePrefix;

    @Column(name = "title_suffix", length = 200)
    private String titleSuffix;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Column(name = "total_participants_count", nullable = false)
    private Long totalParticipantsCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PollStatus status;

    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<PollOption> options = new ArrayList<>();

    @Builder
    public Poll(String titlePrefix, String titleSuffix, LocalDate targetDate, PollStatus status) {
        this.titlePrefix = titlePrefix;
        this.titleSuffix = titleSuffix;
        this.targetDate = targetDate;
        this.status = status;
        this.totalParticipantsCount = 0L;
    }

    public void update(String titlePrefix, String titleSuffix, LocalDate targetDate, PollStatus status) {
        if (titlePrefix != null) this.titlePrefix = titlePrefix;
        if (titleSuffix != null) this.titleSuffix = titleSuffix;
        if (targetDate != null) this.targetDate = targetDate;
        if (status != null) this.status = status;
    }

    public void increaseTotalParticipantsCount() {
        this.totalParticipantsCount = (this.totalParticipantsCount == null ? 0L : this.totalParticipantsCount) + 1L;
    }

    public void decreaseTotalParticipantsCount() {
        long current = this.totalParticipantsCount == null ? 0L : this.totalParticipantsCount;
        this.totalParticipantsCount = Math.max(0L, current - 1L);
    }
}

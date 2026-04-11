package com.swyp.picke.domain.poll.entity;

import com.swyp.picke.domain.poll.enums.PollOptionLabel;
import com.swyp.picke.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "poll_options")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PollOption extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", nullable = false)
    private Poll poll;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PollOptionLabel label;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "vote_count", nullable = false)
    private Long voteCount;

    @Builder
    public PollOption(Poll poll, PollOptionLabel label, String title, Integer displayOrder, Long voteCount) {
        this.poll = poll;
        this.label = label;
        this.title = title;
        this.displayOrder = displayOrder;
        this.voteCount = voteCount == null ? 0L : voteCount;
    }


    public void update(String title) {
        if (title != null) this.title = title;
        if (displayOrder != null) this.displayOrder = displayOrder;
    }

    public void increaseVoteCount() {
        this.voteCount = (this.voteCount == null ? 0L : this.voteCount) + 1;
    }

    public void decreaseVoteCount() {
        if (this.voteCount != null && this.voteCount > 0) {
            this.voteCount--;
        }
    }
}

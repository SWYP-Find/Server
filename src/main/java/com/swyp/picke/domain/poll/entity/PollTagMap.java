package com.swyp.picke.domain.poll.entity;

import com.swyp.picke.domain.tag.entity.CategoryTag;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "poll_tags")
@IdClass(PollTagMapId.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PollTagMap {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", nullable = false)
    private Poll poll;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_tag_id", nullable = false)
    private CategoryTag categoryTag;

    @Builder
    public PollTagMap(Poll poll, CategoryTag categoryTag) {
        this.poll = poll;
        this.categoryTag = categoryTag;
    }
}


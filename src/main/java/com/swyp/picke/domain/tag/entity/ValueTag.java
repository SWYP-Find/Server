package com.swyp.picke.domain.tag.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "value_tags")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ValueTag {

    @Id
    @Column(name = "tag_id")
    private Long tagId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    @Builder
    public ValueTag(Tag tag) {
        this.tag = tag;
    }
}


package com.swyp.app.domain.tag.entity;

import com.swyp.app.domain.tag.enums.TagType;
import com.swyp.app.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "tags")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tag extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "tag_id", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TagType type;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public Tag(String name, TagType type) {
        this.name = name;
        this.type = type;
        this.deletedAt = null;
    }

    public void updateTag(String name, TagType type) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (type != null) {
            this.type = type;
        }
    }

    // 소프트 삭제 메서드
    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }
}
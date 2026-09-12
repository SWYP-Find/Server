package com.swyp.picke.domain.scenario.entity;

import com.swyp.picke.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 철학자 이름 → Fish Audio 보이스(reference_id) 매핑.
 * 붙여넣기 파서가 시나리오 발화자에 맞는 보이스를 채울 때 조회한다.
 */
@Getter
@Entity
@Table(name = "philosopher_voice")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PhilosopherVoice extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "reference_id", nullable = false, length = 64)
    private String referenceId;

    @Column(name = "voice_label", length = 100)
    private String voiceLabel;

    /** 철학자 이미지 저장 키(S3/Railway Bucket). 없으면 프론트가 기본 이미지로 대체해야 한다. */
    @Column(name = "image_key", length = 500)
    private String imageKey;

    @Column(length = 255)
    private String note;

    @Builder
    public PhilosopherVoice(String name, String referenceId, String voiceLabel, String imageKey, String note) {
        this.name = name;
        this.referenceId = referenceId;
        this.voiceLabel = voiceLabel;
        this.imageKey = imageKey;
        this.note = note;
    }

    public void update(String referenceId, String voiceLabel, String imageKey, String note) {
        if (referenceId != null && !referenceId.isBlank()) {
            this.referenceId = referenceId;
        }
        if (voiceLabel != null) {
            this.voiceLabel = voiceLabel;
        }
        if (imageKey != null) {
            this.imageKey = imageKey;
        }
        if (note != null) {
            this.note = note;
        }
    }
}

package com.swyp.app.domain.scenario.entity;

import com.swyp.app.domain.scenario.enums.SpeakerType;
import com.swyp.app.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "scenario_scripts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Script extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "script_id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "node_id")
    private ScenarioNode node;

    @Column(name = "start_time_ms")
    private Integer startTimeMs;

    @Enumerated(EnumType.STRING)
    @Column(name = "speaker_type", nullable = false)
    private SpeakerType speakerType;

    @Column(name = "speaker_name")
    private String speakerName;

    @Column(columnDefinition = "TEXT")
    private String text; // SSML 태그가 포함된 텍스트

    @Builder
    public Script(Integer startTimeMs, SpeakerType speakerType, String speakerName, String text) {
        this.startTimeMs = startTimeMs;
        this.speakerType = speakerType;
        this.speakerName = speakerName;
        this.text = text;
    }

    public void assignNode(ScenarioNode node) {
        this.node = node;
    }

    public void updateStartTimeMs(Integer startTimeMs) {
        this.startTimeMs = startTimeMs;
    }
}
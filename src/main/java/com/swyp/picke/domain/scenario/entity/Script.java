package com.swyp.picke.domain.scenario.entity;

import com.swyp.picke.domain.scenario.enums.SpeakerType;
import com.swyp.picke.domain.scenario.enums.Tone;
import com.swyp.picke.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "scenario_scripts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Script extends BaseEntity {

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
    private String text; // 오디오 효과 태그([sighing] 등)가 인라인으로 포함된 텍스트

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Tone tone = Tone.NEUTRAL;

    @Column(name = "audio_url")
    private String audioUrl;

    @Builder
    public Script(Integer startTimeMs, SpeakerType speakerType, String speakerName, String text, Tone tone) {
        this.startTimeMs = startTimeMs;
        this.speakerType = speakerType;
        this.speakerName = speakerName;
        this.text = text;
        this.tone = tone == null ? Tone.NEUTRAL : tone;
    }

    public void updateAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public void updateContent(SpeakerType speakerType, String speakerName, String newText, Tone newTone) {
        this.speakerType = speakerType;
        this.speakerName = speakerName;
        this.text = newText;
        this.tone = newTone == null ? Tone.NEUTRAL : newTone;
        this.audioUrl = null;
    }

    public void assignNode(ScenarioNode node) {
        this.node = node;
    }

    public void updateStartTimeMs(Integer startTimeMs) {
        this.startTimeMs = startTimeMs;
    }
}
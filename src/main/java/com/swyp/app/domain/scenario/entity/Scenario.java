package com.swyp.app.domain.scenario.entity;

import com.swyp.app.domain.battle.entity.Battle;
import com.swyp.app.domain.scenario.enums.AudioPathType;
import com.swyp.app.domain.scenario.enums.CreatorType;
import com.swyp.app.domain.scenario.enums.ScenarioStatus;
import com.swyp.app.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.*;

@Entity
@Table(name = "scenarios")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Scenario extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "scenario_id", updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "battle_id", nullable = false)
    private Battle battle;

    @Column(name = "is_interactive", nullable = false)
    private Boolean isInteractive;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScenarioStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CreatorType creatorType;

    @ElementCollection
    @CollectionTable(name = "scenario_audios", joinColumns = @JoinColumn(name = "scenario_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "path_key")
    @Column(name = "audio_url")
    private Map<AudioPathType, String> audios = new EnumMap<>(AudioPathType.class);

    @OneToMany(mappedBy = "scenario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ScenarioNode> nodes = new ArrayList<>();

    @Builder
    public Scenario(Battle battle, Boolean isInteractive, ScenarioStatus status, CreatorType creatorType) {
        this.battle = battle;
        this.isInteractive = isInteractive;
        this.status = status;
        this.creatorType = creatorType;
    }

    public void updateStatus(ScenarioStatus status) {
        this.status = status;
    }

    public void addAudioUrl(AudioPathType type, String url) {
        this.audios.put(type, url);
    }

    public void addNode(ScenarioNode node) {
        this.nodes.add(node);
        node.assignScenario(this);
    }

    public void clearNodes() {
        this.nodes.clear();
    }
}
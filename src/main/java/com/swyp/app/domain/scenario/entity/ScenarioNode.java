package com.swyp.app.domain.scenario.entity;

import com.swyp.app.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "scenario_nodes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScenarioNode extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "node_id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id")
    private Scenario scenario;

    @Column(name = "node_name")
    private String nodeName;

    @Column(name = "is_start_node")
    private Boolean isStartNode;

    @Column(name = "audio_duration")
    private Integer audioDuration;

    @Column(name = "auto_next_node_id")
    private UUID autoNextNodeId;

    @OneToMany(mappedBy = "node", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Script> scripts = new ArrayList<>();

    @OneToMany(mappedBy = "node", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InteractiveOption> options = new ArrayList<>();

    @Builder
    public ScenarioNode(String nodeName, Boolean isStartNode, Integer audioDuration, UUID autoNextNodeId) {
        this.nodeName = nodeName;
        this.isStartNode = isStartNode;
        this.audioDuration = audioDuration;
        this.autoNextNodeId = autoNextNodeId;
    }

    public void assignScenario(Scenario scenario) {
        this.scenario = scenario;
    }

    public void addScript(Script script) {
        this.scripts.add(script);
        script.assignNode(this);
    }

    public void addOption(InteractiveOption option) {
        this.options.add(option);
        option.assignNode(this);
    }

    public void updateAutoNextNodeId(UUID autoNextNodeId) {
        this.autoNextNodeId = autoNextNodeId;
    }

    public void updateAudioDuration(Integer audioDuration) {
        this.audioDuration = audioDuration;
    }
}
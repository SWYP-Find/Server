package com.swyp.picke.domain.scenario.entity;

import com.swyp.picke.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "scenario_nodes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScenarioNode extends BaseEntity {

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
    private Long autoNextNodeId;

    @OrderColumn(name = "script_order")
    @OneToMany(mappedBy = "node", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Script> scripts = new ArrayList<>();

    @OrderColumn(name = "option_order")
    @OneToMany(mappedBy = "node", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InteractiveOption> options = new ArrayList<>();

    @Builder
    public ScenarioNode(String nodeName, Boolean isStartNode, Integer audioDuration, Long autoNextNodeId) {
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

    public void updateBasicInfo(Boolean isStartNode) {
        this.isStartNode = isStartNode;
    }

    public void clearOptionsAndLinks() {
        this.autoNextNodeId = null;
        this.options.clear();
    }

    public void updateAutoNextNodeId(Long autoNextNodeId) {
        this.autoNextNodeId = autoNextNodeId;
    }

    public void updateAudioDuration(Integer audioDuration) {
        this.audioDuration = audioDuration;
    }
}

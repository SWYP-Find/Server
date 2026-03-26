package com.swyp.app.domain.scenario.entity;

import com.swyp.app.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "scenario_options")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InteractiveOption extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "node_id")
    private ScenarioNode node;

    private String label;

    @Column(name = "next_node_id")
    private Long nextNodeId;

    @Builder
    public InteractiveOption(String label, Long nextNodeId) {
        this.label = label;
        this.nextNodeId = nextNodeId;
    }

    public void assignNode(ScenarioNode node) {
        this.node = node;
    }
}
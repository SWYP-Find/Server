package com.swyp.app.domain.scenario.entity;

import com.swyp.app.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "scenario_options")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InteractiveOption extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "option_id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "node_id")
    private ScenarioNode node;

    private String label;

    @Column(name = "next_node_id")
    private UUID nextNodeId;

    @Builder
    public InteractiveOption(String label, UUID nextNodeId) {
        this.label = label;
        this.nextNodeId = nextNodeId;
    }

    public void assignNode(ScenarioNode node) {
        this.node = node;
    }
}
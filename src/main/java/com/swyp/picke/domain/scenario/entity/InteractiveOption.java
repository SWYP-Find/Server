package com.swyp.picke.domain.scenario.entity;

import com.swyp.picke.global.common.BaseEntity;
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

    /** 선택의 시간 화면에 보여줄 선택지 설명(예: "첫 만남에는 그에 걸맞은 격조와 분위기가 필수다, 유죄!"). */
    @Column(length = 255)
    private String title;

    @Column(name = "next_node_id")
    private Long nextNodeId;

    @Builder
    public InteractiveOption(String label, String title, Long nextNodeId) {
        this.label = label;
        this.title = title;
        this.nextNodeId = nextNodeId;
    }

    public void assignNode(ScenarioNode node) {
        this.node = node;
    }
}
package com.swyp.picke.domain.classroom.entity;

import com.swyp.picke.domain.vote.entity.BattleVote;
import com.swyp.picke.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 클래스 중간투표. 기존 콘텐츠의 선택 지점(checkpoint = 선택지가 있는 시나리오 노드)마다 1건 (PRD §7 "checkpoint_id별 기록", §16 "phase+checkpoint_id 고유").
 * <ul>
 *   <li>선택지는 시나리오 선택지 ID 로 저장한다. 콘텐츠의 선택지는 배틀 선택지와 문구가 달라 배틀 선택지로 바꾸지 않는다</li>
 *   <li>시나리오를 다시 저장하면 노드·선택지 ID 가 바뀔 수 있어 FK 를 걸지 않고, 리포트용으로 선택 당시 문구를 같이 저장한다</li>
 * </ul>
 */
@Getter
@Entity
@Table(
        name = "class_mid_votes",
        uniqueConstraints = @UniqueConstraint(name = "uk_class_mid_votes_checkpoint", columnNames = {"vote_id", "checkpoint_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClassMidVote extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vote_id", nullable = false)
    private BattleVote vote;

    /** 선택 지점 = scenario_nodes.id */
    @Column(name = "checkpoint_id", nullable = false)
    private Long checkpointId;

    /** scenario_options.id */
    @Column(name = "option_id", nullable = false)
    private Long optionId;

    /** 선택 당시 선택지 문구 */
    @Column(name = "option_label", nullable = false, length = 255)
    private String optionLabel;

    @Builder
    private ClassMidVote(BattleVote vote, Long checkpointId, Long optionId, String optionLabel) {
        this.vote = vote;
        this.checkpointId = checkpointId;
        this.optionId = optionId;
        this.optionLabel = optionLabel;
    }

    public void change(Long optionId, String optionLabel) {
        this.optionId = optionId;
        this.optionLabel = optionLabel;
    }
}

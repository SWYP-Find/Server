package com.swyp.picke.domain.classroom.service;

import com.swyp.picke.domain.scenario.entity.InteractiveOption;
import com.swyp.picke.domain.scenario.entity.ScenarioNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ClassMidVoteServiceTest {

    @Test
    @DisplayName("필수 중간투표: 시작 노드부터 고른 경로의 선택 지점만 요구한다")
    void firstMissingCheckpoint_followsChosenPath() {
        // 1(시작, 선택: 11→2 / 12→3) → 2(자동→4) / 3(선택: 13→4) → 4(끝)
        ScenarioNode start = node(1L, true, null);
        ScenarioNode left = node(2L, false, 4L);
        ScenarioNode right = node(3L, false, null);
        ScenarioNode end = node(4L, false, null);
        option(start, 11L, 2L);
        option(start, 12L, 3L);
        option(right, 13L, 4L);
        List<ScenarioNode> nodes = List.of(start, left, right, end);

        assertThat(ClassMidVoteService.firstMissingCheckpoint(nodes, Map.of())).contains(1L);
        // 왼쪽 경로는 선택 지점이 하나뿐
        assertThat(ClassMidVoteService.firstMissingCheckpoint(nodes, Map.of(1L, 11L))).isEmpty();
        // 오른쪽 경로는 3번 지점도 답해야 한다
        assertThat(ClassMidVoteService.firstMissingCheckpoint(nodes, Map.of(1L, 12L))).contains(3L);
        assertThat(ClassMidVoteService.firstMissingCheckpoint(nodes, Map.of(1L, 12L, 3L, 13L))).isEmpty();
        // 시나리오 수정으로 사라진 선택지는 답하지 않은 것으로 본다
        assertThat(ClassMidVoteService.firstMissingCheckpoint(nodes, Map.of(1L, 99L))).contains(1L);
    }

    @Test
    @DisplayName("순환 링크가 있어도 멈춘다")
    void firstMissingCheckpoint_cycleGuard() {
        ScenarioNode a = node(1L, true, 2L);
        ScenarioNode b = node(2L, false, 1L);
        assertThat(ClassMidVoteService.firstMissingCheckpoint(List.of(a, b), Map.of())).isEqualTo(Optional.empty());
    }

    private static ScenarioNode node(Long id, boolean start, Long autoNext) {
        ScenarioNode node = ScenarioNode.builder().nodeName("n" + id).isStartNode(start).autoNextNodeId(autoNext).build();
        ReflectionTestUtils.setField(node, "id", id);
        return node;
    }

    private static void option(ScenarioNode node, Long id, Long next) {
        InteractiveOption option = InteractiveOption.builder().label("o" + id).title("t" + id).nextNodeId(next).build();
        ReflectionTestUtils.setField(option, "id", id);
        node.addOption(option);
    }
}

package com.swyp.app.domain.scenario.util;

import com.swyp.app.domain.scenario.entity.ScenarioNode;
import java.util.ArrayList;
import java.util.List;

public class PathFinder {

    public static List<List<ScenarioNode>> findAllPaths(List<ScenarioNode> nodes) {
        List<List<ScenarioNode>> allPaths = new ArrayList<>();

        ScenarioNode startNode = nodes.stream()
                .filter(ScenarioNode::getIsStartNode)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("시작 노드가 없습니다."));

        // 시작 노드부터 탐색 시작
        dfs(startNode, new ArrayList<>(), allPaths, nodes);
        return allPaths;
    }

    private static void dfs(ScenarioNode current, List<ScenarioNode> currentPath,
                            List<List<ScenarioNode>> allPaths, List<ScenarioNode> allNodes) {

        // 순환 참조 방지: 현재 경로에 이미 이 노드가 포함되어 있다면 중단 (Cycle Detection)
        if (currentPath.contains(current)) {
            // 순환이 발견된 지점까지의 경로를 저장하고 싶다면 여기서 추가, 아니면 그냥 리턴
            allPaths.add(new ArrayList<>(currentPath));
            return;
        }

        currentPath.add(current);

        boolean hasNext = false;

        // 1. 선택지(Options)가 있는 경우
        if (current.getOptions() != null && !current.getOptions().isEmpty()) {
            for (var option : current.getOptions()) {
                if (option.getNextNodeId() != null) {
                    ScenarioNode nextNode = findNodeById(allNodes, option.getNextNodeId());
                    if (nextNode != null) {
                        hasNext = true;
                        // 새 경로 리스트를 넘겨서 분기 처리 (기존 path 영향 방지)
                        dfs(nextNode, new ArrayList<>(currentPath), allPaths, allNodes);
                    }
                }
            }
        }

        // 2. 자동 다음 노드(AutoNext)가 있는 경우 (Options가 없을 때만 작동하도록 else if)
        else if (current.getAutoNextNodeId() != null) {
            ScenarioNode nextNode = findNodeById(allNodes, current.getAutoNextNodeId());
            if (nextNode != null) {
                hasNext = true;
                dfs(nextNode, new ArrayList<>(currentPath), allPaths, allNodes);
            }
        }

        // 3. 더 이상 갈 곳이 없는 경우 (종료 노드)
        if (!hasNext) {
            allPaths.add(new ArrayList<>(currentPath));
        }
    }

    private static ScenarioNode findNodeById(List<ScenarioNode> nodes, Long id) {
        return nodes.stream()
                .filter(n -> n.getId().equals(id))
                .findFirst()
                .orElse(null);
    }
}
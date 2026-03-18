package com.swyp.app.domain.scenario.util;

import com.swyp.app.domain.scenario.entity.ScenarioNode;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PathFinder {
    public static List<List<ScenarioNode>> findAllPaths(List<ScenarioNode> nodes) {
        List<List<ScenarioNode>> allPaths = new ArrayList<>();
        ScenarioNode startNode = nodes.stream()
                .filter(ScenarioNode::getIsStartNode).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("시작 노드가 없습니다."));

        dfs(startNode, new ArrayList<>(), allPaths, nodes);
        return allPaths;
    }

    private static void dfs(ScenarioNode current, List<ScenarioNode> path,
                            List<List<ScenarioNode>> allPaths, List<ScenarioNode> allNodes) {
        path.add(current);

        if (current.getOptions() != null && !current.getOptions().isEmpty()) {
            for (var option : current.getOptions()) {
                ScenarioNode nextNode = findNodeById(allNodes, option.getNextNodeId());
                if (nextNode != null) dfs(nextNode, new ArrayList<>(path), allPaths, allNodes);
            }
        }
        // 자동 다음 노드가 있는 경우
        else if (current.getAutoNextNodeId() != null) {
            ScenarioNode nextNode = findNodeById(allNodes, current.getAutoNextNodeId());
            if (nextNode != null) dfs(nextNode, path, allPaths, allNodes);
        }
        // 종료 노드인 경우
        else {
            allPaths.add(new ArrayList<>(path));
        }
    }

    private static ScenarioNode findNodeById(List<ScenarioNode> nodes, UUID id) {
        return nodes.stream().filter(n -> n.getId().equals(id)).findFirst().orElse(null);
    }
}
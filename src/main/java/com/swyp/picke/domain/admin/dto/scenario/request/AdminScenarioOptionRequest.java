package com.swyp.picke.domain.admin.dto.scenario.request;

public record AdminScenarioOptionRequest(
        String label,
        String title,
        String nextNodeName
) {}

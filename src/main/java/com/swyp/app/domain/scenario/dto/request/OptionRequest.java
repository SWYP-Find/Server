package com.swyp.app.domain.scenario.dto.request;

public record OptionRequest(
        String label,
        String nextNodeName
) {}
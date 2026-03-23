package com.swyp.app.domain.scenario.dto.response;

import lombok.Builder;

@Builder
public record OptionResponse(
        String label,
        Long nextNodeId
) {}
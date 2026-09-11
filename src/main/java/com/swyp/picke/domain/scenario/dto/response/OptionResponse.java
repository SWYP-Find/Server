package com.swyp.picke.domain.scenario.dto.response;

import lombok.Builder;

@Builder
public record OptionResponse(
        String label,
        String title,
        Long nextNodeId
) {}

package com.swyp.app.domain.scenario.dto.response;

import lombok.Builder;
import java.util.UUID;

@Builder
public record OptionResponse(
        String label,
        UUID nextNodeId
) {}
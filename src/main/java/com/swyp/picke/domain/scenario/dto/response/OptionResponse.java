package com.swyp.picke.domain.scenario.dto.response;

import lombok.Builder;

@Builder
public record OptionResponse(
        Long optionId,
        String label,
        String title,
        Long nextNodeId
) {}

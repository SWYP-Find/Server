package com.swyp.picke.domain.admin.dto.philosophervoice.request;

import jakarta.validation.constraints.NotBlank;

public record PhilosopherVoiceRequest(
        @NotBlank String name,
        @NotBlank String referenceId,
        String voiceLabel,
        String imageKey,
        String note
) {}

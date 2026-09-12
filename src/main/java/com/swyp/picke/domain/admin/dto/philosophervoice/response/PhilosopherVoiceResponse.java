package com.swyp.picke.domain.admin.dto.philosophervoice.response;

import com.swyp.picke.domain.scenario.entity.PhilosopherVoice;

public record PhilosopherVoiceResponse(
        Long id,
        String name,
        String referenceId,
        String voiceLabel,
        String imageKey,
        String note
) {
    public static PhilosopherVoiceResponse from(PhilosopherVoice entity) {
        return new PhilosopherVoiceResponse(
                entity.getId(),
                entity.getName(),
                entity.getReferenceId(),
                entity.getVoiceLabel(),
                entity.getImageKey(),
                entity.getNote()
        );
    }
}

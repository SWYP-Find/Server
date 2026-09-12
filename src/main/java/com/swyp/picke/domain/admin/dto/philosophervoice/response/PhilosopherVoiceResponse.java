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
    /**
     * @param imageUrl {@code entity.getImageKey()} 를 {@code ResourceUrlProvider.getImageUrl(FileCategory.PHILOSOPHER, ...)}
     *                 로 변환한 호출 가능한 경로. raw key 를 그대로 내려주지 않는다(다른 이미지 필드와 동일한 규칙).
     */
    public static PhilosopherVoiceResponse from(PhilosopherVoice entity, String imageUrl) {
        return new PhilosopherVoiceResponse(
                entity.getId(),
                entity.getName(),
                entity.getReferenceId(),
                entity.getVoiceLabel(),
                imageUrl,
                entity.getNote()
        );
    }
}

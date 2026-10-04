package com.swyp.picke.domain.admin.dto.battle.request;

import jakarta.validation.constraints.NotBlank;

/** 저장 전 폼 내용으로도 후보를 만들 수 있게 battleId 대신 배틀 내용을 받는다. */
public record AdminBattleThumbnailCandidateRequest(
        @NotBlank String title,
        String summary,
        String description,
        Side optionA,
        Side optionB
) {
    public record Side(String title, String stance) {}
}

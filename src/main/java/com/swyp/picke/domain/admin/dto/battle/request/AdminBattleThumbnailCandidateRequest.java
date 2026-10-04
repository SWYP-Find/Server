package com.swyp.picke.domain.admin.dto.battle.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 저장 전 폼 내용으로도 후보를 만들 수 있게 battleId 대신 배틀 내용을 받는다.
 * 썸네일은 제목(주제)을 그리므로 A/B 입장은 받지 않는다. 요약/설명은 제목의 맥락으로만 쓴다.
 */
public record AdminBattleThumbnailCandidateRequest(
        @NotBlank String title,
        String summary,
        String description
) {}

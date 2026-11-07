package com.swyp.picke.domain.admin.dto.battle.response;

import java.util.List;

/**
 * 썸네일 후보 목록.
 * key 는 배틀 저장 시 thumbnailUrl 로 그대로 넘기고, previewUrl 은 화면 미리보기용(presigned)이다.
 */
public record AdminBattleThumbnailCandidatesResponse(List<Candidate> candidates) {

    public record Candidate(String key, String previewUrl) {}
}

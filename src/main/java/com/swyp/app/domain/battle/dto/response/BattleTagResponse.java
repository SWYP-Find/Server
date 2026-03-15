package com.swyp.app.domain.battle.dto.response;
import com.swyp.app.domain.tag.enums.TagType;

import java.util.UUID;

/**
 * 유저 - 배틀 태그 응답
 * 역할: 화면 곳곳에 쓰이는 #예술 #철학 등의 태그 정보를 담습니다.
 */

public record BattleTagResponse(
        UUID tagId,    // 태그 고유 ID
        String name,   // 태그 명칭
        TagType type   // 태그 카테고리 (CATEGORY, PHILOSOPHER, VALUE)
) {}
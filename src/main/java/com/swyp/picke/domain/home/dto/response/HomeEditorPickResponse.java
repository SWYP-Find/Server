package com.swyp.picke.domain.home.dto.response;

import com.swyp.picke.domain.battle.dto.response.BattleTagResponse;

import java.util.List;

public record HomeEditorPickResponse(
        Long battleId,
        String thumbnailUrl,
        String optionATitle,
        String optionBTitle,
        String title,
        String summary,
        List<BattleTagResponse> tags,
        Integer viewCount
) {}

package com.swyp.picke.domain.battle.dto.response;

import java.util.List;

public record BattleListResponse(
        List<BattleSimpleResponse> items,
        int currentPage,
        int totalPages,
        long totalItems
) {}
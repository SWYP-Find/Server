package com.swyp.picke.domain.home.dto.response;

public record HomeTodayQuizResponse(
        Long battleId,
        String title,
        String summary,
        Long participantsCount,
        String itemA,
        String itemADesc,
        String itemB,
        String itemBDesc
) {}

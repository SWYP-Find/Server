package com.swyp.picke.domain.battle.dto.response;

public record OptionStatResponse(
        Long optionId,
        String title,
        Long voteCount,
        Double ratio
) {}

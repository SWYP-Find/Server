package com.swyp.picke.domain.battle.dto.response;

public record TodayOptionResponse(
        Long optionId,
        String title,
        String representative,
        String stance,
        String imageUrl
) {}

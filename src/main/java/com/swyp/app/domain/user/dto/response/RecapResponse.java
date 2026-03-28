package com.swyp.app.domain.user.dto.response;

import com.swyp.app.domain.user.entity.PhilosopherType;

import java.util.List;

public record RecapResponse(
        PhilosopherCard myCard,
        PhilosopherCard bestMatchCard,
        PhilosopherCard worstMatchCard,
        Scores scores,
        PreferenceReport preferenceReport
) {

    public record PhilosopherCard(
            PhilosopherType philosopherType,
            String philosopherLabel,
            String typeName,
            String description,
            String imageUrl
    ) {
    }

    public record Scores(
            int principle,
            int reason,
            int individual,
            int change,
            int inner,
            int ideal
    ) {
    }

    public record PreferenceReport(
            int totalParticipation,
            int opinionChanges,
            int battleWinRate,
            List<FavoriteTopic> favoriteTopics
    ) {
    }

    public record FavoriteTopic(
            int rank,
            String tagName,
            int participationCount
    ) {
    }
}

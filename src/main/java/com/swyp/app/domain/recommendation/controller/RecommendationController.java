package com.swyp.app.domain.recommendation.controller;

import com.swyp.app.domain.recommendation.dto.response.RecommendationListResponse;
import com.swyp.app.domain.recommendation.service.RecommendationService;
import com.swyp.app.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/battles/{battleId}/recommendations/similar")
    public ApiResponse<RecommendationListResponse> getSimilarBattles(@PathVariable UUID battleId) {
        return ApiResponse.onSuccess(recommendationService.getSimilarBattles(battleId));
    }
}

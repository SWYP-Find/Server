package com.swyp.app.domain.recommendation.controller;

import com.swyp.app.domain.recommendation.dto.response.RecommendationListResponse;
import com.swyp.app.domain.recommendation.service.RecommendationService;
import com.swyp.app.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "추천 (Recommendation)", description = "배틀 추천 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @Operation(summary = "흥미 기반 배틀 추천 조회", description = "특정 배틀 기반으로 흥미로운 배틀 목록을 추천합니다.")
    @GetMapping("/battles/{battleId}/recommendations/interesting")
    public ApiResponse<RecommendationListResponse> getInterestingBattles(
            @PathVariable Long battleId,
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.onSuccess(recommendationService.getInterestingBattles(battleId, userId));
    }
}

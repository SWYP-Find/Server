package com.swyp.picke.domain.classroom.controller;

import com.swyp.picke.domain.battle.enums.BattleDifficulty;
import com.swyp.picke.domain.classroom.dto.response.ClassBattleRecommendationResponse;
import com.swyp.picke.domain.classroom.service.ClassBattleRecommendationService;
import com.swyp.picke.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "클래스 Picke 배틀 추천 API", description = "새 클래스 만들기에서 조건에 맞는 기존 배틀(TTS 포함) 추천")
@RestController
@RequestMapping("/api/v1/class-battle-recommendations")
@RequiredArgsConstructor
public class ClassBattleRecommendationController {

    private final ClassBattleRecommendationService recommendationService;

    @Operation(summary = "조건에 맞는 Picke 배틀 추천",
            description = "공개된 배틀 중 어드민이 클래스 추천을 켠 배틀에서 수준이 같고 카테고리가 겹치는 배틀을 최대 3개 돌려줍니다. "
                    + "조건에 맞는 배틀이 적으면 적은 대로(0개 포함) 내려갑니다. 수준 누락, 카테고리 0개·4개 이상·잘못된 값, "
                    + "주제 50자 초과는 400 CLASS_400_RECOMMEND_CONDITION.")
    @GetMapping
    public ApiResponse<ClassBattleRecommendationResponse> recommend(
            @Parameter(description = "찾고 싶은 주제 (선택, 50자 이하). 쉼표·공백으로 여러 단어", example = "AI, 범죄")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "대상 수준 (필수)", example = "BEGINNER")
            @RequestParam(required = false) BattleDifficulty difficulty,
            @Parameter(description = "카테고리 1~3개 (필수). 한글 이름 또는 enum 이름, 반복 또는 쉼표로 전달", example = "철학,사회")
            @RequestParam(required = false) List<String> categories
    ) {
        return ApiResponse.onSuccess(recommendationService.recommend(keyword, difficulty, categories));
    }
}

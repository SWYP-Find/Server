package com.swyp.picke.domain.search.controller;

import com.swyp.picke.domain.search.dto.response.SearchBattleListResponse;
import com.swyp.picke.domain.search.enums.SearchSortType;
import com.swyp.picke.domain.search.service.SearchService;
import com.swyp.picke.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/search")
@Tag(name = "검색 API", description = "배틀 검색")
public class SearchController {

    private final SearchService searchService;

    @Operation(summary = "배틀 검색", description = "카테고리와 정렬 조건으로 배틀을 검색합니다.")
    @GetMapping("/battles")
    public ApiResponse<SearchBattleListResponse> searchBattles(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) SearchSortType sort,
            @RequestParam(required = false) Integer offset,
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.onSuccess(searchService.searchBattles(category, sort, offset, size));
    }
}

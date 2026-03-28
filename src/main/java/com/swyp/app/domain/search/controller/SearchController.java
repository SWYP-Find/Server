package com.swyp.app.domain.search.controller;

import com.swyp.app.domain.search.dto.response.SearchBattleListResponse;
import com.swyp.app.domain.search.enums.SearchSortType;
import com.swyp.app.domain.search.service.SearchService;
import com.swyp.app.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/search")
public class SearchController {

    private final SearchService searchService;

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

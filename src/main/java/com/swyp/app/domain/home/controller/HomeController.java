package com.swyp.app.domain.home.controller;

import com.swyp.app.domain.home.dto.response.HomeResponse;
import com.swyp.app.domain.home.service.HomeService;
import com.swyp.app.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "홈 API", description = "홈 화면 집계 조회")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class HomeController {

    private final HomeService homeService;

    @Operation(summary = "홈 화면 집계 조회")
    @GetMapping("/home")
    public ApiResponse<HomeResponse> getHome() {
        return ApiResponse.onSuccess(homeService.getHome());
    }
}

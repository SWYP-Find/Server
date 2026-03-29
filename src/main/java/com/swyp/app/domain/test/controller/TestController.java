package com.swyp.app.domain.test.controller;

import com.swyp.app.domain.oauth.jwt.JwtProvider;
import com.swyp.app.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final JwtProvider jwtProvider;

    @GetMapping("/response")
    public ApiResponse<List<String>> testResponse() {
        List<String> teamMembers = List.of("주천수", "팀원2", "팀원3", "팀원4");
        return ApiResponse.onSuccess(teamMembers);
    }

    @GetMapping("/token")
    public ApiResponse<Map<String, String>> getTestToken(
            @RequestParam(defaultValue = "1") Long userId
    ) {
        String token = jwtProvider.createAccessToken(userId, "USER");
        return ApiResponse.onSuccess(Map.of("accessToken", token));
    }
}
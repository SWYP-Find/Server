package com.swyp.app.domain.test.controller;

import com.swyp.app.global.common.response.ApiResponse; // 패키지 경로 확인!
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/response")
    public ApiResponse<List<String>> testResponse() {
        List<String> teamMembers = List.of("주천수", "팀원2", "팀원3", "팀원4");
        return ApiResponse.onSuccess(teamMembers);
    }
}
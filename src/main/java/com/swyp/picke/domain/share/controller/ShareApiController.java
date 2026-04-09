package com.swyp.picke.domain.share.controller;

import com.swyp.picke.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/share")
@RequiredArgsConstructor
public class ShareApiController {

    @Value("${picke.baseUrl}")
    private String baseUrl;

    @GetMapping("/report")
    public ApiResponse<Map<String, String>> getReportShareUrl(@RequestParam Long reportId) {
        String shareUrl = baseUrl + "/report/" + reportId;
        return ApiResponse.onSuccess(Map.of("shareUrl", shareUrl));
    }

    @GetMapping("/battle")
    public ApiResponse<Map<String, String>> getBattleShareUrl(@RequestParam Long battleId) {
        String shareUrl = baseUrl + "/battle/" + battleId;
        return ApiResponse.onSuccess(Map.of("shareUrl", shareUrl));
    }
}

package com.swyp.picke.domain.share.controller;

import com.swyp.picke.domain.share.dto.response.RecapShareKeyResponse;
import com.swyp.picke.domain.share.service.ShareService;
import com.swyp.picke.domain.user.dto.response.RecapResponse;
import com.swyp.picke.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    private final ShareService shareService;

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

    @GetMapping("/recap")
    public ApiResponse<RecapShareKeyResponse> getRecapShareKey() {
        return ApiResponse.onSuccess(shareService.getRecapShareKey());
    }

    @GetMapping("/recap/{shareKey}")
    public ApiResponse<RecapResponse> getSharedRecap(@PathVariable String shareKey) {
        return ApiResponse.onSuccess(shareService.getSharedRecap(shareKey));
    }
}

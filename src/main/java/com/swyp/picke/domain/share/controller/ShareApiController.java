package com.swyp.picke.domain.share.controller;

import com.swyp.picke.domain.share.dto.response.RecapShareKeyResponse;
import com.swyp.picke.domain.share.service.ShareService;
import com.swyp.picke.domain.user.dto.response.RecapResponse;
import com.swyp.picke.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/share")
@RequiredArgsConstructor
@Tag(name = "공유 API", description = "공유 URL 및 리캡 공유 조회")
public class ShareApiController {

    @Value("${picke.baseUrl}")
    private String baseUrl;

    private final ShareService shareService;

    @Operation(summary = "리포트 공유 URL 조회", description = "리포트 공유에 사용할 URL을 조회합니다.")
    @GetMapping("/report")
    public ApiResponse<Map<String, String>> getReportShareUrl(@RequestParam Long reportId) {
        String shareUrl = baseUrl + "/report/" + reportId;
        return ApiResponse.onSuccess(Map.of("shareUrl", shareUrl));
    }

    @Operation(summary = "배틀 공유 URL 조회", description = "배틀 공유에 사용할 URL을 조회합니다.")
    @GetMapping("/battle")
    public ApiResponse<Map<String, String>> getBattleShareUrl(@RequestParam Long battleId) {
        String shareUrl = baseUrl + "/battle/" + battleId;
        return ApiResponse.onSuccess(Map.of("shareUrl", shareUrl));
    }

    @Operation(summary = "리캡 공유 키 발급", description = "내 리캡을 공유하기 위한 공유 키를 발급합니다.")
    @GetMapping("/recap")
    public ApiResponse<RecapShareKeyResponse> getRecapShareKey() {
        return ApiResponse.onSuccess(shareService.getRecapShareKey());
    }

    @Operation(summary = "공유 리캡 조회", description = "공유 키로 공개된 리캡 정보를 조회합니다.")
    @GetMapping("/recap/{shareKey}")
    public ApiResponse<RecapResponse> getSharedRecap(@PathVariable String shareKey) {
        return ApiResponse.onSuccess(shareService.getSharedRecap(shareKey));
    }
}

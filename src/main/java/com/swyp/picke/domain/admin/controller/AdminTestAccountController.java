package com.swyp.picke.domain.admin.controller;

import com.swyp.picke.domain.admin.dto.testaccount.request.CreateTestAccountRequest;
import com.swyp.picke.domain.admin.dto.testaccount.response.TestAccountResponse;
import com.swyp.picke.domain.oauth.service.AuthService;
import com.swyp.picke.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 테스트 계정 API", description = "QA/외부 테스터용 로컬 로그인 계정 발급")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/test-accounts")
@PreAuthorize("hasRole('ADMIN')")
public class AdminTestAccountController {

    private final AuthService authService;

    @Operation(summary = "테스트 계정 생성")
    @PostMapping
    public ApiResponse<TestAccountResponse> createTestAccount(@Valid @RequestBody CreateTestAccountRequest request) {
        return ApiResponse.onSuccess(authService.createLocalTestAccount(request));
    }
}

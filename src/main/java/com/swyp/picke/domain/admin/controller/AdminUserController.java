package com.swyp.picke.domain.admin.controller;

import com.swyp.picke.domain.admin.dto.user.response.AdminUserSearchResponse;
import com.swyp.picke.domain.admin.service.AdminUserService;
import com.swyp.picke.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 유저 API", description = "관리자 페이지에서 유저를 조회하기 위한 API")
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "유저 검색", description = "닉네임/유저태그/이메일(소셜 로그인 유저만 해당)로 유저를 검색한다. 로컬 로그인 유저는 닉네임/유저태그로만 검색된다.")
    @GetMapping("/search")
    public ApiResponse<AdminUserSearchResponse> searchUsers(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.onSuccess(adminUserService.searchUsers(keyword, page, size));
    }
}

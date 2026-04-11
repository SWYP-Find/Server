package com.swyp.picke.domain.admin.controller;

import com.swyp.picke.domain.admin.dto.tag.request.TagRequest;
import com.swyp.picke.domain.admin.dto.tag.response.TagDeleteResponse;
import com.swyp.picke.domain.admin.dto.tag.response.TagResponse;
import com.swyp.picke.domain.admin.service.AdminTagService;
import com.swyp.picke.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 태그 API", description = "관리자 태그 생성, 수정, 삭제")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/tags")
@PreAuthorize("hasRole('ADMIN')")
public class AdminTagController {

    private final AdminTagService adminTagService;

    @Operation(summary = "태그 생성")
    @PostMapping
    public ApiResponse<TagResponse> createTag(@Valid @RequestBody TagRequest request) {
        return ApiResponse.onSuccess(adminTagService.createTag(request));
    }

    @Operation(summary = "태그 수정")
    @PatchMapping("/{tagId}")
    public ApiResponse<TagResponse> updateTag(
            @Parameter(description = "태그 ID", example = "1")
            @PathVariable Long tagId,
            @Valid @RequestBody TagRequest request
    ) {
        return ApiResponse.onSuccess(adminTagService.updateTag(tagId, request));
    }

    @Operation(summary = "태그 삭제")
    @DeleteMapping("/{tagId}")
    public ApiResponse<TagDeleteResponse> deleteTag(
            @Parameter(description = "태그 ID", example = "1")
            @PathVariable Long tagId
    ) {
        return ApiResponse.onSuccess(adminTagService.deleteTag(tagId));
    }
}
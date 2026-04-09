package com.swyp.picke.domain.tag.controller;

import com.swyp.picke.domain.tag.dto.request.TagRequest;
import com.swyp.picke.domain.tag.dto.response.*;
import com.swyp.picke.domain.tag.enums.TagType;
import com.swyp.picke.domain.tag.service.TagService;
import com.swyp.picke.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "태그 (Tag)", description = "태그 조회 및 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class TagController {

    private final TagService tagService;

    @Operation(summary = "태그 목록 조회", description = "전체 태그 목록을 조회합니다. 특정 타입(type)을 지정하여 필터링할 수 있습니다.")
    @GetMapping("/tags")
    public ApiResponse<TagListResponse> getTags(
            @Parameter(description = "필터링할 태그 타입 (예: BATTLE 등)", required = false)
            @RequestParam(name = "type", required = false) TagType type) {

        TagListResponse response = tagService.getTags(type);
        return ApiResponse.onSuccess(response);
    }

    @Operation(summary = "태그 생성 (관리자)", description = "관리자가 새로운 태그를 생성합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/tags")
    public ApiResponse<TagResponse> createTag(
            @Valid @RequestBody TagRequest request) {

        TagResponse response = tagService.createTag(request);
        return ApiResponse.onSuccess(response);
    }

    @Operation(summary = "태그 수정 (관리자)", description = "관리자가 기존 태그의 이름이나 정보를 수정합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/admin/tags/{tag_id}")
    public ApiResponse<TagResponse> updateTag(
            @Parameter(description = "수정할 태그의 ID", example = "1")
            @PathVariable("tag_id") Long tagId,
            @Valid @RequestBody TagRequest request) {

        TagResponse response = tagService.updateTag(tagId, request);
        return ApiResponse.onSuccess(response);
    }

    @Operation(summary = "태그 삭제 (관리자)", description = "관리자가 특정 태그를 삭제합니다. 단, 배틀에 사용 중인 태그는 삭제할 수 없습니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/tags/{tag_id}")
    public ApiResponse<TagDeleteResponse> deleteTag(
            @Parameter(description = "삭제할 태그의 ID", example = "1")
            @PathVariable("tag_id") Long tagId) {

        TagDeleteResponse response = tagService.deleteTag(tagId);
        return ApiResponse.onSuccess(response);
    }
}
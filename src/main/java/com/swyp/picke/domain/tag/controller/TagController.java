package com.swyp.picke.domain.tag.controller;

import com.swyp.picke.domain.tag.dto.response.TagListResponse;
import com.swyp.picke.domain.tag.enums.TagType;
import com.swyp.picke.domain.tag.service.TagService;
import com.swyp.picke.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "태그 API", description = "태그 조회")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class TagController {

    private final TagService tagService;

    @Operation(summary = "태그 목록 조회")
    @GetMapping("/tags")
    public ApiResponse<TagListResponse> getTags(
            @Parameter(description = "태그 타입 필터(선택)", required = false)
            @RequestParam(name = "type", required = false) TagType type) {

        TagListResponse response = tagService.getTags(type);
        return ApiResponse.onSuccess(response);
    }
}
package com.swyp.picke.domain.perspective.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateCommentRequest(
        @NotBlank
        String content
) {}

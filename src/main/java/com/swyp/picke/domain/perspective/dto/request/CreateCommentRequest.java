package com.swyp.picke.domain.perspective.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateCommentRequest(
        @NotBlank
        String content
) {}

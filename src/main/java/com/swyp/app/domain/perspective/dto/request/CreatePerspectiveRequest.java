package com.swyp.app.domain.perspective.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePerspectiveRequest(
        @NotBlank
        @Size(max = 200)
        String content
) {}

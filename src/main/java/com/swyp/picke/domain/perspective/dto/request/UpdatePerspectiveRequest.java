package com.swyp.picke.domain.perspective.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePerspectiveRequest(
        @NotBlank
        @Size(max = 200)
        String content
) {}

package com.swyp.app.domain.tag.dto.request;

import com.swyp.app.domain.tag.enums.TagType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TagRequest(
        @NotBlank(message = "태그 이름을 입력해주세요.")
        String name,

        @NotNull(message = "태그 타입을 선택해주세요.")
        TagType type
) {}
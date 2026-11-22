package com.swyp.picke.domain.admin.dto.funding.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FundingOrderContactUpdateRequest(
        @Schema(description = "후원자 이름") @NotBlank @Size(max = 50) String supporterName,
        @Schema(description = "후원자 이메일", nullable = true) @Email @Size(max = 254) String supporterEmail,
        @Schema(description = "후원자 전화번호", nullable = true) @Size(max = 20) String supporterPhone
) {}

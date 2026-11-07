package com.swyp.picke.domain.ticket.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record ClassTicketRegisterRequest(
        @Schema(description = "이용권 코드. 공백/하이픈/대소문자 무시", example = "PICKE-AB12-CD34")
        @NotBlank(message = "이용권 코드를 입력해 주세요.")
        String code
) {}

package com.swyp.picke.domain.admin.dto.testaccount.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "테스트 계정 생성 요청 객체")
public class CreateTestAccountRequest {

    @NotBlank
    @Schema(description = "테스트 계정 아이디", example = "qa-tester01")
    private String username;

    @NotBlank
    @Schema(description = "테스트 계정 비밀번호", example = "P@ssw0rd!")
    private String password;
}

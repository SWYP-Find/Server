package com.swyp.picke.domain.oauth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "로컬(테스트 계정) 로그인 요청 객체")
public class LocalLoginRequest {

    @NotBlank
    @Schema(description = "관리자가 발급한 테스트 계정 아이디", example = "qa-tester01")
    private String username;

    @NotBlank
    @Schema(description = "테스트 계정 비밀번호", example = "P@ssw0rd!")
    private String password;
}

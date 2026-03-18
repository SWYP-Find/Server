package com.swyp.app.domain.oauth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

//  소셜 API를 호출해서 받아온 사용자 정보를 담는 DTO
@Getter
@AllArgsConstructor
public class OAuthUserInfo {
    private String provider;          // "KAKAO" or "GOOGLE"
    private String providerUserId;    // 소셜 고유 ID
    private String email;             // nullable - 소셜 로그인 시도 시 선택 동의 안함 체크로 인해
}
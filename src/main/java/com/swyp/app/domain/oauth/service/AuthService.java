package com.swyp.app.domain.oauth.service;

import com.swyp.app.domain.oauth.client.GoogleOAuthClient;
import com.swyp.app.domain.oauth.client.KakaoOAuthClient;
import com.swyp.app.domain.oauth.dto.LoginRequest;
import com.swyp.app.domain.oauth.dto.LoginResponse;
import com.swyp.app.domain.oauth.dto.OAuthUserInfo;
import com.swyp.app.domain.oauth.entity.AuthRefreshToken;
import com.swyp.app.domain.oauth.entity.UserSocialAccount;
import com.swyp.app.domain.oauth.jwt.JwtProvider;
import com.swyp.app.domain.oauth.repository.AuthRefreshTokenRepository;
import com.swyp.app.domain.oauth.repository.UserSocialAccountRepository;
import com.swyp.app.domain.user.entity.Role;
import com.swyp.app.domain.user.entity.User;
import com.swyp.app.domain.user.entity.UserStatus;
import com.swyp.app.domain.user.repository.UserRepository;
import com.swyp.app.global.common.exception.CustomException;
import com.swyp.app.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final KakaoOAuthClient kakaoOAuthClient;
    private final GoogleOAuthClient googleOAuthClient;
    private final UserRepository userRepository;
    private final UserSocialAccountRepository socialAccountRepository;
    private final AuthRefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;

    public LoginResponse login(String provider, LoginRequest request) {

        // 1. provider에 따라 소셜 사용자 정보 조회
        OAuthUserInfo oAuthUserInfo = getOAuthUserInfo(provider,
                                                       request.getAuthorizationCode(), request.getRedirectUri());

        // 2. 기존 소셜 계정 조회 → 없으면 신규 유저 생성
        boolean isNewUser = false;
        UserSocialAccount socialAccount = socialAccountRepository
                .findByProviderAndProviderUserId(provider, oAuthUserInfo.getProviderUserId())
                .orElse(null);

        User user;
        if (socialAccount == null) {
            // 신규 유저 생성
            user = User.builder()
                    .userTag(generateUserTag())
                    .role(Role.USER)
                    .status(UserStatus.PENDING)
                    .onboardingCompleted(false)
                    .build();
            userRepository.save(user);

            // 소셜 계정 연결
            socialAccount = UserSocialAccount.builder()
                    .user(user)
                    .provider(provider.toUpperCase())
                    .providerUserId(oAuthUserInfo.getProviderUserId())
                    .providerEmail(oAuthUserInfo.getEmail())
                    .build();
            socialAccountRepository.save(socialAccount);
            isNewUser = true;
        } else {
            user = socialAccount.getUser();
        }

        // 3. 제재 유저 체크
        if (user.getStatus() == UserStatus.BANNED) {
            throw new CustomException(ErrorCode.USER_BANNED);
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new CustomException(ErrorCode.USER_SUSPENDED);
        }

        // 4. 기존 refresh token 삭제 후 새로 발급
        refreshTokenRepository.deleteByUser(user);

        String accessToken = jwtProvider.createAccessToken(user.getId());
        String refreshToken = jwtProvider.createRefreshToken();

        // 5. refresh token 해시해서 저장
        refreshTokenRepository.save(AuthRefreshToken.builder()
                                            .user(user)
                                            .tokenHash(hashToken(refreshToken))
                                            .expiresAt(LocalDateTime.now().plusDays(30))
                                            .build());

        return new LoginResponse(
                accessToken,
                refreshToken,
                user.getUserTag(),
                isNewUser,
                user.getStatus().name()
        );
    }

    public LoginResponse refresh(String refreshToken) {

        // 1. refresh token 해시해서 DB 조회
        String tokenHash = hashToken(refreshToken);
        AuthRefreshToken authRefreshToken = refreshTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_REFRESH_TOKEN_EXPIRED));

        // 2. 만료 여부 확인
        if (authRefreshToken.isExpired()) {
            refreshTokenRepository.delete(authRefreshToken);
            throw new CustomException(ErrorCode.AUTH_REFRESH_TOKEN_EXPIRED);
        }

        // 3. 기존 토큰 삭제 후 새 토큰 발급
        User user = authRefreshToken.getUser();
        refreshTokenRepository.delete(authRefreshToken);

        String newAccessToken = jwtProvider.createAccessToken(user.getId());
        String newRefreshToken = jwtProvider.createRefreshToken();

        // 4. 새 refresh token 저장
        refreshTokenRepository.save(AuthRefreshToken.builder()
                                            .user(user)
                                            .tokenHash(hashToken(newRefreshToken))
                                            .expiresAt(LocalDateTime.now().plusDays(30))
                                            .build());

        return new LoginResponse(
                newAccessToken,
                newRefreshToken,
                user.getUserTag(),
                false,
                user.getStatus().name()
        );
    }

    public void logout(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        refreshTokenRepository.deleteByUser(user);
    }

    public void withdraw(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        refreshTokenRepository.deleteByUser(user);
        user.delete();
    }

    // provider에 따라 소셜 사용자 정보 조회
    private OAuthUserInfo getOAuthUserInfo(String provider, String code, String redirectUri) {
        return switch (provider.toUpperCase()) {
            case "KAKAO" -> {
                String token = kakaoOAuthClient.getAccessToken(code, redirectUri);
                yield kakaoOAuthClient.getUserInfo(token);
            }
            case "GOOGLE" -> {
                String token = googleOAuthClient.getAccessToken(code, redirectUri);
                yield googleOAuthClient.getUserInfo(token);
            }
            default -> throw new CustomException(ErrorCode.INVALID_PROVIDER);
        };
    }

    // user_tag 랜덤 생성
    private String generateUserTag() {
        return "pique-" + UUID.randomUUID().toString().substring(0, 8);
    }

    // refresh token 해시
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("토큰 해시 실패", e);
        }
    }
}
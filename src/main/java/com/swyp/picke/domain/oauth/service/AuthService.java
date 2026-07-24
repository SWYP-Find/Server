package com.swyp.picke.domain.oauth.service;

import com.swyp.picke.domain.oauth.client.AppleOAuthClient;
import com.swyp.picke.domain.oauth.client.GoogleOAuthClient;
import com.swyp.picke.domain.oauth.client.KakaoOAuthClient;
import com.swyp.picke.domain.admin.dto.testaccount.request.CreateTestAccountRequest;
import com.swyp.picke.domain.admin.dto.testaccount.response.TestAccountResponse;
import com.swyp.picke.domain.oauth.dto.LocalLoginRequest;
import com.swyp.picke.domain.oauth.dto.LoginRequest;
import com.swyp.picke.domain.oauth.dto.LoginResponse;
import com.swyp.picke.domain.oauth.dto.OAuthUserInfo;
import com.swyp.picke.domain.oauth.dto.WithdrawRequest;
import com.swyp.picke.domain.oauth.entity.AuthRefreshToken;
import com.swyp.picke.domain.oauth.entity.UserLocalAccount;
import com.swyp.picke.domain.oauth.entity.UserSocialAccount;
import com.swyp.picke.domain.oauth.jwt.JwtProvider;
import com.swyp.picke.domain.oauth.repository.AuthRefreshTokenRepository;
import com.swyp.picke.domain.oauth.repository.UserLocalAccountRepository;
import com.swyp.picke.domain.oauth.repository.UserSocialAccountRepository;
import com.swyp.picke.domain.user.enums.CharacterType;
import com.swyp.picke.domain.user.enums.CreditType;
import com.swyp.picke.domain.user.enums.UserRole;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.entity.UserProfile;
import com.swyp.picke.domain.user.entity.UserSettings;
import com.swyp.picke.domain.user.enums.UserStatus;
import com.swyp.picke.domain.user.entity.UserTendencyScore;
import com.swyp.picke.domain.user.entity.UserWithdrawal;
import com.swyp.picke.domain.user.repository.UserProfileRepository;
import com.swyp.picke.domain.user.repository.UserRepository;
import com.swyp.picke.domain.user.repository.UserSettingsRepository;
import com.swyp.picke.domain.user.repository.UserTendencyScoreRepository;
import com.swyp.picke.domain.user.repository.UserWithdrawalRepository;
import com.swyp.picke.domain.user.service.CreditService;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    static final List<String> DEFAULT_NICKNAME_PREFIXES = List.of(
            "사색하는",
            "질문하는",
            "성찰하는",
            "탐구하는",
            "고요한",
            "사유하는",
            "관조하는",
            "통찰하는",
            "본질을찾는",
            "의문을품은",
            "진리를좇는",
            "철학하는",
            "깊이를품은",
            "내면을걷는",
            "사유에잠긴",
            "유쾌한",
            "대담한",
            "조용한",
            "엉뚱한",
            "날카로운",
            "느긋한",
            "반짝이는",
            "다정한",
            "성실한",
            "호기심많은",
            "재빠른"
    );

    private final KakaoOAuthClient kakaoOAuthClient;
    private final GoogleOAuthClient googleOAuthClient;
    private final AppleOAuthClient appleOAuthClient;
    private final UserRepository userRepository;
    private final UserSocialAccountRepository socialAccountRepository;
    private final UserLocalAccountRepository localAccountRepository;
    private final AuthRefreshTokenRepository refreshTokenRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final UserTendencyScoreRepository userTendencyScoreRepository;
    private final UserWithdrawalRepository userWithdrawalRepository;
    private final JwtProvider jwtProvider;
    private final CreditService creditService;
    private final PasswordEncoder passwordEncoder;

    public LoginResponse login(String provider, LoginRequest request) {

        // 1. Provider 가공 시 대문자 통일 및 문자열 정제 전처리 적용
        String providerUpper = provider.trim().toUpperCase();

        // 2. 소셜 사용자 정보 조회 통합 엔드포인트 연동
        OAuthUserInfo oAuthUserInfo = getOAuthUserInfo(providerUpper, request);

        // 3. 기존 소셜 계정 조회
        UserSocialAccount socialAccount = socialAccountRepository
                .findByProviderAndProviderUserId(providerUpper, oAuthUserInfo.getProviderUserId())
                .orElse(null);

        boolean isNewUser = false;

        User user;
        if (socialAccount == null) {
            // 신규 유저 생성
            user = User.builder()
                    .userTag(generateUserTag())
                    .role(UserRole.USER)
                    .status(UserStatus.ACTIVE)
                    .build();
            userRepository.save(user);
            initializeUserDomain(user);

            // 소셜 계정 연결 및 생성 파이프라인 수행
            socialAccount = UserSocialAccount.builder()
                    .user(user)
                    .provider(providerUpper)
                    .providerUserId(oAuthUserInfo.getProviderUserId())
                    .providerEmail(oAuthUserInfo.getEmail())
                    .build();

            // 5. 애플 로그인인 경우 애플용 리프레시 토큰 발급 후 엔티티에 기록 위임
            if ("APPLE".equals(providerUpper)) {
                String appleRefreshToken = appleOAuthClient.getAppleRefreshToken(request.getAuthorizationCode());
                socialAccount.updateAppleRefreshToken(appleRefreshToken);
            }

            socialAccountRepository.save(socialAccount);

            // 크레딧 지급 (30 크레딧)
            creditService.addCredit(user.getId(), CreditType.DEFAULT_CREDIT, user.getId());
            isNewUser = true;
        } else {
            user = socialAccount.getUser();

            // 6. 기존 가입 유저더라도 애플의 경우 리프레시 토큰 데이터 갱신 보강
            if ("APPLE".equals(providerUpper)) {
                String appleRefreshToken = appleOAuthClient.getAppleRefreshToken(request.getAuthorizationCode());
                socialAccount.updateAppleRefreshToken(appleRefreshToken);
                socialAccountRepository.save(socialAccount);
            }
        }

        return issueTokensAndBuildResponse(user, isNewUser);
    }

    public LoginResponse loginLocal(LocalLoginRequest request) {
        UserLocalAccount localAccount = localAccountRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.getPassword(), localAccount.getPasswordHash())) {
            throw new CustomException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }

        return issueTokensAndBuildResponse(localAccount.getUser(), false);
    }

    public TestAccountResponse createLocalTestAccount(CreateTestAccountRequest request) {
        if (localAccountRepository.existsByUsername(request.getUsername())) {
            throw new CustomException(ErrorCode.LOCAL_ACCOUNT_USERNAME_DUPLICATED);
        }

        User user = User.builder()
                .userTag(generateUserTag())
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.save(user);
        initializeUserDomain(user);

        localAccountRepository.save(UserLocalAccount.builder()
                                             .user(user)
                                             .username(request.getUsername())
                                             .passwordHash(passwordEncoder.encode(request.getPassword()))
                                             .build());

        creditService.addCredit(user.getId(), CreditType.DEFAULT_CREDIT, user.getId());

        return new TestAccountResponse(request.getUsername(), user.getUserTag());
    }

    private LoginResponse issueTokensAndBuildResponse(User user, boolean isNewUser) {
        // 제재 유저 체크
        if (user.getStatus() == UserStatus.BANNED) {
            throw new CustomException(ErrorCode.USER_BANNED);
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new CustomException(ErrorCode.USER_SUSPENDED);
        }

        // 기존 refresh token 삭제 후 새로 발급
        refreshTokenRepository.deleteByUser(user);

        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getRole().name());
        String refreshToken = jwtProvider.createRefreshToken();

        // refresh token 해시해서 저장
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

        // refresh token 해시해서 DB 조회
        String tokenHash = hashToken(refreshToken);
        AuthRefreshToken authRefreshToken = refreshTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_REFRESH_TOKEN_EXPIRED));

        // 만료 여부 확인
        if (authRefreshToken.isExpired()) {
            refreshTokenRepository.delete(authRefreshToken);
            throw new CustomException(ErrorCode.AUTH_REFRESH_TOKEN_EXPIRED);
        }

        // 기존 token 삭제 후 새 토큰 발급
        User user = authRefreshToken.getUser();
        refreshTokenRepository.delete(authRefreshToken);

        String newAccessToken = jwtProvider.createAccessToken(user.getId(), user.getRole().name());
        String newRefreshToken = jwtProvider.createRefreshToken();

        // 새 refresh token 저장
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

    public void withdraw(Long userId, WithdrawRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        refreshTokenRepository.deleteByUser(user);

        if (user.getStatus() == UserStatus.DELETED) {
            throw new CustomException(ErrorCode.USER_ALREADY_WITHDRAWN);
        }

        if (!userWithdrawalRepository.existsByUser_Id(userId)) {
            userWithdrawalRepository.save(UserWithdrawal.builder()
                                                  .user(user)
                                                  .reason(request.reason())
                                                  .build());
        }

        socialAccountRepository.findByUser(user).ifPresent(socialAccount -> {
            if ("APPLE".equalsIgnoreCase(socialAccount.getProvider()) && socialAccount.getAppleRefreshToken() != null) {
                // 2. appleOAuthClient 컴포넌트 내부에 회원탈퇴(Revoke) 기능 호출 위임 추가
                appleOAuthClient.revokeAppleAccount(socialAccount.getAppleRefreshToken());
            }
            socialAccountRepository.delete(socialAccount);
        });

        user.delete();
    }

    // 7. 애플 분기 조건 스위치 매핑 구조 보완 및 파라미터 규격 최적화
    private OAuthUserInfo getOAuthUserInfo(String provider, LoginRequest request) {
        return switch (provider.toUpperCase()) {
            case "TRACK" -> throw new CustomException(ErrorCode.INVALID_PROVIDER);
            case "KAKAO" -> {
                String token = kakaoOAuthClient.getAccessToken(request.getAuthorizationCode(), request.getRedirectUri());
                yield kakaoOAuthClient.getUserInfo(token);
            }
            case "GOOGLE" -> {
                String token = googleOAuthClient.getAccessToken(request.getAuthorizationCode(), request.getRedirectUri());
                yield googleOAuthClient.getUserInfo(token);
            }
            case "APPLE" -> {
                // 애플 로그인 검증은 클라이언트가 보낸 identityToken을 직통으로 위임
                yield appleOAuthClient.getUserInfo(request.getIdentityToken());
            }
            default -> throw new CustomException(ErrorCode.INVALID_PROVIDER);
        };
    }

    // user_tag 랜덤 생성
    private String generateUserTag() {
        return "pique-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private void initializeUserDomain(User user) {
        CharacterType characterType = CharacterType.random();

        userProfileRepository.save(UserProfile.builder()
                                           .user(user)
                                           .nickname(generateDefaultNickname(characterType))
                                           .characterType(characterType)
                                           .mannerTemperature(BigDecimal.valueOf(36.5))
                                           .build());

        userSettingsRepository.save(UserSettings.builder()
                                            .user(user)
                                            .newBattleEnabled(false)
                                            .battleResultEnabled(true)
                                            .commentReplyEnabled(true)
                                            .newCommentEnabled(false)
                                            .contentLikeEnabled(false)
                                            .marketingEventEnabled(true)
                                            .build());

        userTendencyScoreRepository.save(UserTendencyScore.builder()
                                                 .user(user)
                                                 .principle(0)
                                                 .reason(0)
                                                 .individual(0)
                                                 .change(0)
                                                 .inner(0)
                                                 .ideal(0)
                                                 .build());
    }

    private String generateDefaultNickname(CharacterType characterType) {
        String prefix = DEFAULT_NICKNAME_PREFIXES.get(
                ThreadLocalRandom.current().nextInt(DEFAULT_NICKNAME_PREFIXES.size())
        );
        return prefix + characterType.getLabel();
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
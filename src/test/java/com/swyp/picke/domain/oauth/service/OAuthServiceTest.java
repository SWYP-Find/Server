package com.swyp.picke.domain.oauth.service;

import com.swyp.picke.domain.oauth.client.GoogleOAuthClient;
import com.swyp.picke.domain.oauth.client.KakaoOAuthClient;
import com.swyp.picke.domain.oauth.dto.LoginRequest;
import com.swyp.picke.domain.oauth.dto.LoginResponse;
import com.swyp.picke.domain.oauth.dto.OAuthUserInfo;
import com.swyp.picke.domain.oauth.dto.WithdrawRequest;
import com.swyp.picke.domain.oauth.repository.AuthRefreshTokenRepository;
import com.swyp.picke.domain.oauth.repository.UserSocialAccountRepository;
import com.swyp.picke.domain.oauth.jwt.JwtProvider;
import com.swyp.picke.domain.user.enums.CharacterType;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.entity.UserProfile;
import com.swyp.picke.domain.user.entity.UserWithdrawal;
import com.swyp.picke.domain.user.enums.UserRole;
import com.swyp.picke.domain.user.enums.UserStatus;
import com.swyp.picke.domain.user.enums.WithdrawalReason;
import com.swyp.picke.domain.user.repository.UserProfileRepository;
import com.swyp.picke.domain.user.repository.UserRepository;
import com.swyp.picke.domain.user.repository.UserSettingsRepository;
import com.swyp.picke.domain.user.repository.UserTendencyScoreRepository;
import com.swyp.picke.domain.user.repository.UserWithdrawalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuthServiceTest {

    @Mock private KakaoOAuthClient kakaoOAuthClient;
    @Mock private GoogleOAuthClient googleOAuthClient;
    @Mock private UserRepository userRepository;
    @Mock private UserSocialAccountRepository socialAccountRepository;
    @Mock private AuthRefreshTokenRepository refreshTokenRepository;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private UserSettingsRepository userSettingsRepository;
    @Mock private UserTendencyScoreRepository userTendencyScoreRepository;
    @Mock private UserWithdrawalRepository userWithdrawalRepository;
    @Mock private JwtProvider jwtProvider;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        // 수동 주입으로 안정성 확보
        authService = new AuthService(
                kakaoOAuthClient, googleOAuthClient, userRepository,
                socialAccountRepository, refreshTokenRepository,
                userProfileRepository, userSettingsRepository, userTendencyScoreRepository,
                userWithdrawalRepository,
                jwtProvider
        );
    }

    @Test
    void login_카카오_기존유저_로그인_성공() {
        // 1. 준비 (Given)
        String provider = "KAKAO";
        LoginRequest request = new LoginRequest("auth-code", "redirect-uri");
        OAuthUserInfo userInfo = new OAuthUserInfo("kakao_123", "bex@test.com", "profile_url");

        // 유저 엔티티에 ID가 없으므로 식별자 필드만 세팅 (UserTag 등)
        User user = User.builder()
                .userTag("pique-test")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();

        // 2. Mock 설정 (anyString()을 사용하여 null이 아닌 어떤 문자열이든 대응)
        when(kakaoOAuthClient.getAccessToken(anyString(), anyString())).thenReturn("mock-access-token");
        when(kakaoOAuthClient.getUserInfo(anyString())).thenReturn(userInfo); // 여기서 null이 안 들어가게 고정

        var socialAccount = mock(com.swyp.picke.domain.oauth.entity.UserSocialAccount.class);
        when(socialAccount.getUser()).thenReturn(user);
        when(socialAccountRepository.findByProviderAndProviderUserId(anyString(), anyString()))
                .thenReturn(Optional.of(socialAccount));

        // ID가 없더라도 createAccessToken의 첫 번째 인자가 무엇이든 통과하게 any() 사용
        when(jwtProvider.createAccessToken(any(), anyString())).thenReturn("jwt-access");
        when(jwtProvider.createRefreshToken()).thenReturn("jwt-refresh");

        // 3. 실행 (When)
        LoginResponse response = authService.login(provider, request);

        // 4. 검증 (Then)
        assertThat(response.getAccessToken()).isEqualTo("jwt-access");
        assertThat(response.isNewUser()).isFalse();
        verify(refreshTokenRepository).save(any());
    }

    @Test
    void login_구글_신규유저_기본_user_domain_초기화() {
        String provider = "GOOGLE";
        LoginRequest request = new LoginRequest("auth-code", "redirect-uri");
        OAuthUserInfo userInfo = new OAuthUserInfo("google_123", "new@test.com", "profile_url");

        User savedUser = User.builder()
                .userTag("pique-test")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();

        when(googleOAuthClient.getAccessToken(anyString(), anyString())).thenReturn("mock-access-token");
        when(googleOAuthClient.getUserInfo(anyString())).thenReturn(userInfo);
        when(socialAccountRepository.findByProviderAndProviderUserId(anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtProvider.createAccessToken(any(), anyString())).thenReturn("jwt-access");
        when(jwtProvider.createRefreshToken()).thenReturn("jwt-refresh");

        LoginResponse response = authService.login(provider, request);

        assertThat(response.isNewUser()).isTrue();
        ArgumentCaptor<UserProfile> profileCaptor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileRepository).save(profileCaptor.capture());
        verify(userSettingsRepository).save(any());
        verify(userTendencyScoreRepository).save(any());

        UserProfile savedProfile = profileCaptor.getValue();
        CharacterType characterType = savedProfile.getCharacterType();

        assertThat(characterType).isNotNull();
        assertThat(savedProfile.getNickname()).endsWith(characterType.getLabel());
        assertThat(savedProfile.getNickname()).isNotEqualTo(savedUser.getUserTag());
        assertThat(AuthService.DEFAULT_NICKNAME_PREFIXES)
                .anyMatch(prefix -> savedProfile.getNickname().startsWith(prefix));
    }

    @Test
    void withdraw_탈퇴사유를_저장하고_사용자를_삭제처리한다() {
        User user = User.builder()
                .userTag("pique-test")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userWithdrawalRepository.existsByUser_Id(1L)).thenReturn(false);

        authService.withdraw(1L, new WithdrawRequest(WithdrawalReason.NO_TIME));

        verify(refreshTokenRepository).deleteByUser(user);

        ArgumentCaptor<UserWithdrawal> withdrawalCaptor = ArgumentCaptor.forClass(UserWithdrawal.class);
        verify(userWithdrawalRepository).save(withdrawalCaptor.capture());
        assertThat(withdrawalCaptor.getValue().getReason()).isEqualTo(WithdrawalReason.NO_TIME);

        assertThat(user.getStatus()).isEqualTo(UserStatus.DELETED);
        assertThat(user.getDeletedAt()).isNotNull();
    }

    @Test
    void withdraw_이미_탈퇴이력이_있으면_중복저장하지_않는다() {
        User user = User.builder()
                .userTag("pique-test")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userWithdrawalRepository.existsByUser_Id(1L)).thenReturn(true);

        authService.withdraw(1L, new WithdrawRequest(WithdrawalReason.OTHER));

        verify(refreshTokenRepository).deleteByUser(user);
        verify(userWithdrawalRepository, never()).save(any());
        assertThat(user.getStatus()).isEqualTo(UserStatus.DELETED);
    }
}

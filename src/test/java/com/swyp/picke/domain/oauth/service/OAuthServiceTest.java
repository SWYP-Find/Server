package com.swyp.picke.domain.oauth.service;

import com.swyp.picke.domain.oauth.client.GoogleOAuthClient;
import com.swyp.picke.domain.oauth.client.KakaoOAuthClient;
import com.swyp.picke.domain.oauth.client.AppleOAuthClient;
import com.swyp.picke.domain.oauth.dto.LoginRequest;
import com.swyp.picke.domain.oauth.dto.LoginResponse;
import com.swyp.picke.domain.oauth.dto.OAuthUserInfo;
import com.swyp.picke.domain.oauth.dto.WithdrawRequest;
import com.swyp.picke.domain.oauth.entity.UserSocialAccount;
import com.swyp.picke.domain.oauth.jwt.JwtProvider;
import com.swyp.picke.domain.oauth.repository.AuthRefreshTokenRepository;
import com.swyp.picke.domain.oauth.repository.UserSocialAccountRepository;
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
import com.swyp.picke.domain.user.service.CreditService;
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
    @Mock private AppleOAuthClient appleOAuthClient;
    @Mock private UserRepository userRepository;
    @Mock private UserSocialAccountRepository socialAccountRepository;
    @Mock private AuthRefreshTokenRepository refreshTokenRepository;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private UserSettingsRepository userSettingsRepository;
    @Mock private UserTendencyScoreRepository userTendencyScoreRepository;
    @Mock private UserWithdrawalRepository userWithdrawalRepository;
    @Mock private JwtProvider jwtProvider;
    @Mock private CreditService creditService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        // 수동 주입으로 안정성 확보
        authService = new AuthService(
                kakaoOAuthClient, googleOAuthClient, appleOAuthClient, userRepository,
                socialAccountRepository, refreshTokenRepository,
                userProfileRepository, userSettingsRepository, userTendencyScoreRepository,
                userWithdrawalRepository,
                jwtProvider, creditService
        );
    }

    @Test
    void login_카카오_기존유저_로그인_성공() {
        // 1. 준비 (Given)
        String provider = "KAKAO";
        LoginRequest request = new LoginRequest("auth-code", "redirect-uri", null);
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
        LoginRequest request = new LoginRequest("auth-code", "redirect-uri", null);
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
    void login_애플_신규유저_로그인_및_리프레시토큰_저장_성공() {
        // given
        String provider = "APPLE";
        LoginRequest request = new LoginRequest("auth-code", "redirect-uri", "identity-token");
        OAuthUserInfo userInfo = new OAuthUserInfo("apple_123", "apple@test.com", null);

        User savedUser = User.builder()
                .userTag("pique-apple")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();

        when(appleOAuthClient.getUserInfo(anyString())).thenReturn(userInfo);
        when(socialAccountRepository.findByProviderAndProviderUserId(anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(appleOAuthClient.getAppleRefreshToken(anyString())).thenReturn("apple-refresh-token");
        when(jwtProvider.createAccessToken(any(), anyString())).thenReturn("jwt-access");
        when(jwtProvider.createRefreshToken()).thenReturn("jwt-refresh");

        // when
        LoginResponse response = authService.login(provider, request);

        // then
        assertThat(response.isNewUser()).isTrue();
        ArgumentCaptor<UserSocialAccount> socialCaptor = ArgumentCaptor.forClass(UserSocialAccount.class);
        verify(socialAccountRepository).save(socialCaptor.capture());

        // 애플 연동 데이터 내부에 리프레시 토큰이 정상 주입되었는지 확인
        assertThat(socialCaptor.getValue().getAppleRefreshToken()).isEqualTo("apple-refresh-token");
    }

    @Test
    void login_애플_기존유저_로그인_및_리프레시토큰_갱신_성공() {
        // given
        String provider = "APPLE";
        LoginRequest request = new LoginRequest("auth-code", "redirect-uri", "identity-token");
        OAuthUserInfo userInfo = new OAuthUserInfo("apple_123", "apple@test.com", null);

        User user = User.builder()
                .userTag("pique-apple")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();

        UserSocialAccount socialAccount = spy(UserSocialAccount.builder()
                                                      .user(user)
                                                      .provider("APPLE")
                                                      .providerUserId("apple_123")
                                                      .build());

        when(appleOAuthClient.getUserInfo(anyString())).thenReturn(userInfo);
        when(socialAccountRepository.findByProviderAndProviderUserId(anyString(), anyString()))
                .thenReturn(Optional.of(socialAccount));
        when(appleOAuthClient.getAppleRefreshToken(anyString())).thenReturn("apple-new-refresh-token");
        when(jwtProvider.createAccessToken(any(), anyString())).thenReturn("jwt-access");
        when(jwtProvider.createRefreshToken()).thenReturn("jwt-refresh");

        // when
        LoginResponse response = authService.login(provider, request);

        // then
        assertThat(response.isNewUser()).isFalse();
        // 새 토큰으로 필드 업데이트 위임이 돌았는지 확인
        verify(socialAccount).updateAppleRefreshToken("apple-new-refresh-token");
        verify(socialAccountRepository).save(socialAccount);
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

    @Test
    void withdraw_애플유저_탈퇴시_연동해제_API를_호출하고_소셜계정을_제거한다() {
        // given
        Long userId = 1L;
        User user = User.builder()
                .userTag("pique-apple")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();

        UserSocialAccount socialAccount = UserSocialAccount.builder()
                .user(user)
                .provider("APPLE")
                .providerUserId("apple_123")
                .build();
        socialAccount.updateAppleRefreshToken("apple-stored-token");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userWithdrawalRepository.existsByUser_Id(userId)).thenReturn(false);
        when(socialAccountRepository.findByUser(user)).thenReturn(Optional.of(socialAccount));

        // when
        authService.withdraw(userId, new WithdrawRequest(WithdrawalReason.NO_TIME));

        // then
        // 1. 애플 연동 해제 전용 클라이언트 API가 정상 전사 되었는지 추적
        verify(appleOAuthClient, times(1)).revokeAppleAccount("apple-stored-token");
        // 2. 가독성을 위해 DB 연동 테이블 매핑 관계에서 소셜 레코드가 소멸하는지 추적
        verify(socialAccountRepository, times(1)).delete(socialAccount);
        assertThat(user.getStatus()).isEqualTo(UserStatus.DELETED);
    }
}
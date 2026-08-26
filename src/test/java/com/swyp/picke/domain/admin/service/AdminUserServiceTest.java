package com.swyp.picke.domain.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.swyp.picke.domain.admin.dto.user.response.AdminUserSearchResponse;
import com.swyp.picke.domain.oauth.entity.UserSocialAccount;
import com.swyp.picke.domain.oauth.repository.UserSocialAccountRepository;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.enums.UserRole;
import com.swyp.picke.domain.user.enums.UserStatus;
import com.swyp.picke.domain.user.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSocialAccountRepository userSocialAccountRepository;

    @InjectMocks
    private AdminUserService adminUserService;

    private User newUser(long id, String userTag, String nickname) {
        User user = User.builder()
                .userTag(userTag)
                .nickname(nickname)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    @DisplayName("닉네임 또는 유저태그로 매칭된 유저를 조회한다")
    void searchUsers_matchesByNicknameOrUserTag() {
        User user = newUser(1L, "picke_abcd", "민초러버");
        Pageable pageable = PageRequest.of(0, 20);
        when(userRepository.searchByNicknameOrUserTag(anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user), pageable, 1));
        when(userSocialAccountRepository.findByProviderEmailContaining(anyString())).thenReturn(List.of());
        when(userSocialAccountRepository.findByUser_IdIn(any())).thenReturn(List.of());

        AdminUserSearchResponse response = adminUserService.searchUsers("민초", 0, 20);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).userId()).isEqualTo(1L);
        assertThat(response.items().get(0).nickname()).isEqualTo("민초러버");
        assertThat(response.items().get(0).email()).isNull();
    }

    @Test
    @DisplayName("이메일로 매칭된 소셜 로그인 유저를 조회하고 이메일을 함께 내려준다")
    void searchUsers_matchesByEmail() {
        User user = newUser(2L, "picke_efgh", "질문왕");
        UserSocialAccount socialAccount = UserSocialAccount.builder()
                .user(user)
                .provider("GOOGLE")
                .providerUserId("google-1")
                .providerEmail("user@gmail.com")
                .build();
        Pageable pageable = PageRequest.of(0, 20);
        when(userRepository.searchByNicknameOrUserTag(anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));
        when(userSocialAccountRepository.findByProviderEmailContaining(anyString()))
                .thenReturn(List.of(socialAccount));
        when(userSocialAccountRepository.findByUser_IdIn(any())).thenReturn(List.of(socialAccount));

        AdminUserSearchResponse response = adminUserService.searchUsers("gmail", 0, 20);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).userId()).isEqualTo(2L);
        assertThat(response.items().get(0).email()).isEqualTo("user@gmail.com");
    }

    @Test
    @DisplayName("로컬 로그인 유저는 소셜 계정이 없어도 닉네임/유저태그로 조회된다")
    void searchUsers_localLoginUser_hasNoEmail() {
        User user = newUser(3L, "picke_ijkl", "로컬유저");
        Pageable pageable = PageRequest.of(0, 20);
        when(userRepository.searchByNicknameOrUserTag(anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user), pageable, 1));
        when(userSocialAccountRepository.findByProviderEmailContaining(anyString())).thenReturn(List.of());
        when(userSocialAccountRepository.findByUser_IdIn(any())).thenReturn(List.of());

        AdminUserSearchResponse response = adminUserService.searchUsers("로컬", 0, 20);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).email()).isNull();
    }
}

package com.swyp.picke.domain.share.service;

import com.swyp.picke.domain.share.dto.response.RecapShareKeyResponse;
import com.swyp.picke.domain.user.dto.response.RecapResponse;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.entity.UserProfile;
import com.swyp.picke.domain.user.enums.CharacterType;
import com.swyp.picke.domain.user.enums.UserRole;
import com.swyp.picke.domain.user.enums.UserStatus;
import com.swyp.picke.domain.user.repository.UserProfileRepository;
import com.swyp.picke.domain.user.service.MypageService;
import com.swyp.picke.domain.user.service.UserService;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShareServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private MypageService mypageService;

    @InjectMocks
    private ShareService shareService;

    @Test
    @DisplayName("리캡 공유 키는 최초 1회 생성 후 재사용된다")
    void getRecapShareKey_generates_and_reuses_key() {
        User user = createUser(1L, "tag");
        UserProfile profile = createProfile(user, "nick");

        when(userService.findCurrentUser()).thenReturn(user);
        when(userService.findUserProfile(1L)).thenReturn(profile);
        when(mypageService.findRecapByUserId(1L)).thenReturn(createRecap());

        RecapShareKeyResponse first = shareService.getRecapShareKey();
        RecapShareKeyResponse second = shareService.getRecapShareKey();

        assertThat(first.shareKey()).isNotBlank();
        assertThat(second.shareKey()).isEqualTo(first.shareKey());
        assertThat(profile.getRecapShareKey()).isEqualTo(first.shareKey());
    }

    @Test
    @DisplayName("리캡이 없으면 공유 키를 발급하지 않는다")
    void getRecapShareKey_throws_when_recap_missing() {
        User user = createUser(1L, "tag");
        UserProfile profile = createProfile(user, "nick");

        when(userService.findCurrentUser()).thenReturn(user);
        when(userService.findUserProfile(1L)).thenReturn(profile);
        when(mypageService.findRecapByUserId(1L)).thenReturn(null);

        assertThatThrownBy(() -> shareService.getRecapShareKey())
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RECAP_NOT_FOUND);
    }

    @Test
    @DisplayName("공유 키로 타인의 리캡을 조회한다")
    void getSharedRecap_returns_recap() {
        User user = createUser(2L, "other");
        UserProfile profile = createProfile(user, "other-nick");
        profile.updateRecapShareKey("share-key");
        RecapResponse recap = createRecap();

        when(userProfileRepository.findByRecapShareKey("share-key")).thenReturn(Optional.of(profile));
        when(mypageService.findRecapByUserId(2L)).thenReturn(recap);

        RecapResponse response = shareService.getSharedRecap("share-key");

        assertThat(response).isSameAs(recap);
    }

    private User createUser(Long id, String userTag) {
        User user = User.builder()
                .userTag(userTag)
                .nickname("nickname")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private UserProfile createProfile(User user, String nickname) {
        return UserProfile.builder()
                .user(user)
                .nickname(nickname)
                .characterType(CharacterType.OWL)
                .mannerTemperature(BigDecimal.valueOf(36.5))
                .build();
    }

    private RecapResponse createRecap() {
        return new RecapResponse(null, null, null, null, null);
    }
}

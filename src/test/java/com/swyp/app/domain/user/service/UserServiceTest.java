package com.swyp.app.domain.user.service;

import com.swyp.app.domain.user.dto.request.UpdateUserProfileRequest;
import com.swyp.app.domain.user.dto.response.MyProfileResponse;
import com.swyp.app.domain.user.dto.response.UserSummary;
import com.swyp.app.domain.user.entity.CharacterType;
import com.swyp.app.domain.user.entity.User;
import com.swyp.app.domain.user.entity.UserProfile;
import com.swyp.app.domain.user.entity.UserRole;
import com.swyp.app.domain.user.entity.UserSettings;
import com.swyp.app.domain.user.entity.UserStatus;
import com.swyp.app.domain.user.entity.UserTendencyScore;
import com.swyp.app.domain.user.repository.UserProfileRepository;
import com.swyp.app.domain.user.repository.UserRepository;
import com.swyp.app.domain.user.repository.UserSettingsRepository;
import com.swyp.app.domain.user.repository.UserTendencyScoreRepository;
import com.swyp.app.global.common.exception.CustomException;
import com.swyp.app.global.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserProfileRepository userProfileRepository;
    @Mock
    private UserSettingsRepository userSettingsRepository;
    @Mock
    private UserTendencyScoreRepository userTendencyScoreRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("가장 최근 사용자를 반환한다")
    void findCurrentUser_returns_latest_user() {
        User user = createUser(1L, "testTag");
        when(userRepository.findTopByOrderByIdDesc()).thenReturn(Optional.of(user));

        User result = userService.findCurrentUser();

        assertThat(result.getUserTag()).isEqualTo("testTag");
    }

    @Test
    @DisplayName("사용자가 없으면 예외를 던진다")
    void findCurrentUser_throws_when_no_user() {
        when(userRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findCurrentUser())
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));
    }

    @Test
    @DisplayName("사용자 요약정보를 반환한다")
    void findSummaryById_returns_user_summary() {
        User user = createUser(1L, "summaryTag");
        UserProfile profile = createProfile(user, "nick", CharacterType.OWL);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));

        UserSummary summary = userService.findSummaryById(1L);

        assertThat(summary.userTag()).isEqualTo("summaryTag");
        assertThat(summary.nickname()).isEqualTo("nick");
        assertThat(summary.characterType()).isEqualTo("OWL");
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 요약정보 조회 시 예외를 던진다")
    void findSummaryById_throws_when_not_found() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findSummaryById(999L))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));
    }

    @Test
    @DisplayName("닉네임과 캐릭터를 수정한다")
    void updateMyProfile_updates_nickname_and_character() {
        User user = createUser(1L, "myTag");
        UserProfile profile = createProfile(user, "oldNick", CharacterType.OWL);

        when(userRepository.findTopByOrderByIdDesc()).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));

        UpdateUserProfileRequest request = new UpdateUserProfileRequest("newNick", CharacterType.FOX);
        MyProfileResponse response = userService.updateMyProfile(request);

        assertThat(response.userTag()).isEqualTo("myTag");
        assertThat(response.nickname()).isEqualTo("newNick");
        assertThat(response.characterType()).isEqualTo(CharacterType.FOX);
    }

    @Test
    @DisplayName("프로필을 반환한다")
    void findUserProfile_returns_profile() {
        User user = createUser(1L, "tag");
        UserProfile profile = createProfile(user, "nick", CharacterType.BEAR);

        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));

        UserProfile result = userService.findUserProfile(1L);

        assertThat(result.getNickname()).isEqualTo("nick");
        assertThat(result.getCharacterType()).isEqualTo(CharacterType.BEAR);
    }

    @Test
    @DisplayName("설정을 반환한다")
    void findUserSettings_returns_settings() {
        User user = createUser(1L, "tag");
        UserSettings settings = UserSettings.builder()
                .user(user)
                .newBattleEnabled(true)
                .battleResultEnabled(false)
                .commentReplyEnabled(true)
                .newCommentEnabled(false)
                .contentLikeEnabled(true)
                .marketingEventEnabled(false)
                .build();

        when(userSettingsRepository.findByUserId(1L)).thenReturn(Optional.of(settings));

        UserSettings result = userService.findUserSettings(1L);

        assertThat(result.isNewBattleEnabled()).isTrue();
        assertThat(result.isBattleResultEnabled()).isFalse();
    }

    @Test
    @DisplayName("성향점수를 반환한다")
    void findUserTendencyScore_returns_score() {
        User user = createUser(1L, "tag");
        UserTendencyScore score = UserTendencyScore.builder()
                .user(user)
                .principle(10)
                .reason(20)
                .individual(30)
                .change(40)
                .inner(50)
                .ideal(60)
                .build();

        when(userTendencyScoreRepository.findByUserId(1L)).thenReturn(Optional.of(score));

        UserTendencyScore result = userService.findUserTendencyScore(1L);

        assertThat(result.getPrinciple()).isEqualTo(10);
        assertThat(result.getReason()).isEqualTo(20);
        assertThat(result.getIdeal()).isEqualTo(60);
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

    private UserProfile createProfile(User user, String nickname, CharacterType characterType) {
        return UserProfile.builder()
                .user(user)
                .nickname(nickname)
                .characterType(characterType)
                .mannerTemperature(BigDecimal.valueOf(36.5))
                .build();
    }
}

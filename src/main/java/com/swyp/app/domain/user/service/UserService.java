package com.swyp.app.domain.user.service;

import com.swyp.app.domain.user.dto.request.UpdateUserProfileRequest;
import com.swyp.app.domain.user.dto.response.MyProfileResponse;
import com.swyp.app.domain.user.dto.response.UserSummary;
import com.swyp.app.domain.user.entity.User;
import com.swyp.app.domain.user.entity.UserProfile;
import com.swyp.app.domain.user.entity.UserSettings;
import com.swyp.app.domain.user.entity.UserTendencyScore;
import com.swyp.app.domain.user.repository.UserProfileRepository;
import com.swyp.app.domain.user.repository.UserRepository;
import com.swyp.app.domain.user.repository.UserSettingsRepository;
import com.swyp.app.domain.user.repository.UserTendencyScoreRepository;
import com.swyp.app.global.common.exception.CustomException;
import com.swyp.app.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final UserTendencyScoreRepository userTendencyScoreRepository;

    @Transactional
    public MyProfileResponse updateMyProfile(UpdateUserProfileRequest request) {
        User user = findCurrentUser();
        UserProfile profile = findUserProfile(user.getId());
        profile.update(request.nickname(), request.characterType());
        return new MyProfileResponse(
                user.getUserTag(),
                profile.getNickname(),
                profile.getCharacterType(),
                profile.getUpdatedAt()
        );
    }

    public UserSummary findSummaryById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        UserProfile profile = findUserProfile(user.getId());
        return new UserSummary(user.getUserTag(), profile.getNickname(), profile.getCharacterType().name());
    }

    public User findCurrentUser() {
        return userRepository.findTopByOrderByIdDesc()
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    public UserProfile findUserProfile(Long userId) {
        return userProfileRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    public UserSettings findUserSettings(Long userId) {
        return userSettingsRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    public UserTendencyScore findUserTendencyScore(Long userId) {
        return userTendencyScoreRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}

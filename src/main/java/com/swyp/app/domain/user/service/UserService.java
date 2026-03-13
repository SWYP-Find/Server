package com.swyp.app.domain.user.service;

import com.swyp.app.domain.user.dto.request.CreateOnboardingProfileRequest;
import com.swyp.app.domain.user.dto.request.UpdateTendencyScoreRequest;
import com.swyp.app.domain.user.dto.request.UpdateUserProfileRequest;
import com.swyp.app.domain.user.dto.request.UpdateUserSettingsRequest;
import com.swyp.app.domain.user.dto.response.BootstrapResponse;
import com.swyp.app.domain.user.dto.response.MyProfileResponse;
import com.swyp.app.domain.user.dto.response.OnboardingProfileResponse;
import com.swyp.app.domain.user.dto.response.TendencyScoreHistoryItemResponse;
import com.swyp.app.domain.user.dto.response.TendencyScoreHistoryResponse;
import com.swyp.app.domain.user.dto.response.TendencyScoreResponse;
import com.swyp.app.domain.user.dto.response.UpdateResultResponse;
import com.swyp.app.domain.user.dto.response.UserProfileResponse;
import com.swyp.app.domain.user.dto.response.UserSettingsResponse;
import com.swyp.app.domain.user.entity.User;
import com.swyp.app.domain.user.entity.UserProfile;
import com.swyp.app.domain.user.entity.UserRole;
import com.swyp.app.domain.user.entity.UserSettings;
import com.swyp.app.domain.user.entity.UserStatus;
import com.swyp.app.domain.user.entity.UserTendencyScore;
import com.swyp.app.domain.user.entity.UserTendencyScoreHistory;
import com.swyp.app.domain.user.repository.UserProfileRepository;
import com.swyp.app.domain.user.repository.UserRepository;
import com.swyp.app.domain.user.repository.UserSettingsRepository;
import com.swyp.app.domain.user.repository.UserTendencyScoreHistoryRepository;
import com.swyp.app.domain.user.repository.UserTendencyScoreRepository;
import com.swyp.app.global.common.exception.CustomException;
import com.swyp.app.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private static final String[] PREFIXES = {"생각하는", "집중하는", "차분한", "기민한", "용감한", "명확한"};
    private static final String[] SUFFIXES = {"올빼미", "여우", "늑대", "사자", "펭귄", "토끼", "고양이", "곰"};
    private static final BigDecimal DEFAULT_MANNER_TEMPERATURE = BigDecimal.valueOf(36.5);
    private static final int DEFAULT_HISTORY_SIZE = 20;

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final UserTendencyScoreRepository userTendencyScoreRepository;
    private final UserTendencyScoreHistoryRepository userTendencyScoreHistoryRepository;

    public BootstrapResponse getBootstrap() {
        return new BootstrapResponse(generateRandomNickname());
    }

    @Transactional
    public OnboardingProfileResponse createOnboardingProfile(CreateOnboardingProfileRequest request) {
        User user = User.builder()
                .userTag(generateUserTag())
                .role(UserRole.USER)
                .status(UserStatus.PENDING)
                .onboardingCompleted(false)
                .build();

        userRepository.save(user);

        UserProfile profile = UserProfile.builder()
                .user(user)
                .nickname(request.nickname())
                .characterType(request.characterType())
                .mannerTemperature(DEFAULT_MANNER_TEMPERATURE)
                .build();

        UserSettings settings = UserSettings.builder()
                .user(user)
                .pushEnabled(true)
                .emailEnabled(false)
                .debateRequestEnabled(true)
                .profilePublic(true)
                .build();

        UserTendencyScore tendencyScore = UserTendencyScore.builder()
                .user(user)
                .score1(0)
                .score2(0)
                .score3(0)
                .score4(0)
                .score5(0)
                .score6(0)
                .build();

        userProfileRepository.save(profile);
        userSettingsRepository.save(settings);
        userTendencyScoreRepository.save(tendencyScore);

        user.completeOnboarding();

        return new OnboardingProfileResponse(
                user.getUserTag(),
                profile.getNickname(),
                profile.getCharacterType(),
                profile.getMannerTemperature(),
                user.getStatus(),
                user.isOnboardingCompleted()
        );
    }

    public UserProfileResponse getUserProfile(String userTag) {
        User user = findUserByTag(userTag);
        UserProfile profile = findUserProfile(user.getId());
        return new UserProfileResponse(user.getUserTag(), profile.getNickname(), profile.getCharacterType(), profile.getMannerTemperature());
    }

    public MyProfileResponse getMyProfile(String userTag) {
        User user = findUserByTag(userTag);
        UserProfile profile = findUserProfile(user.getId());
        return new MyProfileResponse(
                user.getUserTag(),
                profile.getNickname(),
                profile.getCharacterType(),
                profile.getMannerTemperature(),
                profile.getUpdatedAt()
        );
    }

    @Transactional
    public MyProfileResponse updateMyProfile(String userTag, UpdateUserProfileRequest request) {
        User user = findUserByTag(userTag);
        UserProfile profile = findUserProfile(user.getId());
        profile.update(request.nickname(), request.characterType());
        return new MyProfileResponse(
                user.getUserTag(),
                profile.getNickname(),
                profile.getCharacterType(),
                profile.getMannerTemperature(),
                profile.getUpdatedAt()
        );
    }

    public UserSettingsResponse getMySettings(String userTag) {
        UserSettings settings = findUserSettings(findUserByTag(userTag).getId());
        return new UserSettingsResponse(
                settings.isPushEnabled(),
                settings.isEmailEnabled(),
                settings.isDebateRequestEnabled(),
                settings.isProfilePublic()
        );
    }

    @Transactional
    public UpdateResultResponse updateMySettings(String userTag, UpdateUserSettingsRequest request) {
        UserSettings settings = findUserSettings(findUserByTag(userTag).getId());
        settings.update(
                request.pushEnabled(),
                request.emailEnabled(),
                request.debateRequestEnabled(),
                request.profilePublic()
        );
        return new UpdateResultResponse(true);
    }

    @Transactional
    public TendencyScoreResponse updateMyTendencyScores(String userTag, UpdateTendencyScoreRequest request) {
        User user = findUserByTag(userTag);
        UserTendencyScore score = findUserTendencyScore(user.getId());
        score.update(
                request.score1(),
                request.score2(),
                request.score3(),
                request.score4(),
                request.score5(),
                request.score6()
        );

        userTendencyScoreHistoryRepository.save(UserTendencyScoreHistory.builder()
                .user(user)
                .score1(request.score1())
                .score2(request.score2())
                .score3(request.score3())
                .score4(request.score4())
                .score5(request.score5())
                .score6(request.score6())
                .build());

        return new TendencyScoreResponse(
                user.getUserTag(),
                score.getScore1(),
                score.getScore2(),
                score.getScore3(),
                score.getScore4(),
                score.getScore5(),
                score.getScore6(),
                score.getUpdatedAt(),
                true
        );
    }

    public TendencyScoreHistoryResponse getMyTendencyScoreHistory(String userTag, Long cursor, Integer size) {
        User user = findUserByTag(userTag);
        int pageSize = size == null || size <= 0 ? DEFAULT_HISTORY_SIZE : size;
        PageRequest pageable = PageRequest.of(0, pageSize);

        List<UserTendencyScoreHistory> histories = cursor == null
                ? userTendencyScoreHistoryRepository.findByUserOrderByIdDesc(user, pageable)
                : userTendencyScoreHistoryRepository.findByUserAndIdLessThanOrderByIdDesc(user, cursor, pageable);

        List<TendencyScoreHistoryItemResponse> items = histories.stream()
                .map(history -> new TendencyScoreHistoryItemResponse(
                        "ths_%03d".formatted(history.getId()),
                        history.getScore1(),
                        history.getScore2(),
                        history.getScore3(),
                        history.getScore4(),
                        history.getScore5(),
                        history.getScore6(),
                        history.getCreatedAt()
                ))
                .toList();

        Long nextCursor = histories.size() == pageSize ? histories.get(histories.size() - 1).getId() : null;
        return new TendencyScoreHistoryResponse(items, nextCursor);
    }

    public String requireCurrentUserTag(String userTagHeader) {
        if (!StringUtils.hasText(userTagHeader)) {
            throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
        }
        return userTagHeader;
    }

    private User findUserByTag(String userTag) {
        return userRepository.findByUserTag(userTag)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private UserProfile findUserProfile(Long userId) {
        return userProfileRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private UserSettings findUserSettings(Long userId) {
        return userSettingsRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private UserTendencyScore findUserTendencyScore(Long userId) {
        return userTendencyScoreRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private String generateRandomNickname() {
        return PREFIXES[ThreadLocalRandom.current().nextInt(PREFIXES.length)]
                + SUFFIXES[ThreadLocalRandom.current().nextInt(SUFFIXES.length)];
    }

    private String generateUserTag() {
        String candidate;
        do {
            candidate = "sfit4-%d".formatted(ThreadLocalRandom.current().nextInt(1000, 10000));
        } while (userRepository.existsByUserTag(candidate));
        return candidate;
    }
}

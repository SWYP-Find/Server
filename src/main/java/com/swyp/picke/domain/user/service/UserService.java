package com.swyp.picke.domain.user.service;

import com.swyp.picke.domain.battle.service.BattleQueryService;
import com.swyp.picke.domain.user.dto.request.UpdateUserProfileRequest;
import com.swyp.picke.domain.user.dto.response.MyProfileResponse;
import com.swyp.picke.domain.user.dto.response.UserSummary;
import com.swyp.picke.domain.user.entity.PhilosopherType;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.entity.UserProfile;
import com.swyp.picke.domain.user.entity.UserSettings;
import com.swyp.picke.domain.user.entity.UserTendencyScore;
import com.swyp.picke.domain.user.repository.UserProfileRepository;
import com.swyp.picke.domain.user.repository.UserRepository;
import com.swyp.picke.domain.user.repository.UserSettingsRepository;
import com.swyp.picke.domain.user.repository.UserTendencyScoreRepository;
import com.swyp.picke.domain.vote.service.VoteQueryService;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private static final int PHILOSOPHER_CALC_THRESHOLD = 5;

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final UserTendencyScoreRepository userTendencyScoreRepository;
    private final VoteQueryService voteQueryService;
    private final BattleQueryService battleQueryService;

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

    @Transactional
    public PhilosopherType getPhilosopherType(Long userId) {
        UserProfile profile = findUserProfile(userId);

        if (profile.getPhilosopherType() != null) {
            return profile.getPhilosopherType();
        }

        long totalVotes = voteQueryService.countTotalParticipation(userId);
        if (totalVotes < PHILOSOPHER_CALC_THRESHOLD) {
            return PhilosopherType.SOCRATES;
        }

        List<Long> battleIds = voteQueryService.findFirstNBattleIds(userId, PHILOSOPHER_CALC_THRESHOLD);
        return battleQueryService.getTopPhilosopherTagName(battleIds)
                .map(PhilosopherType::fromLabel)
                .map(type -> {
                    profile.updatePhilosopherType(type);
                    return type;
                })
                .orElse(PhilosopherType.SOCRATES);
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
        return userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    public UserSettings findUserSettings(Long userId) {
        return userSettingsRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    public UserTendencyScore findUserTendencyScore(Long userId) {
        return userTendencyScoreRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}

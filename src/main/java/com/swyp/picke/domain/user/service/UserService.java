package com.swyp.picke.domain.user.service;

import com.swyp.picke.domain.battle.service.BattleQueryService;
import com.swyp.picke.domain.user.dto.request.UpdateUserProfileRequest;
import com.swyp.picke.domain.user.dto.response.MyProfileResponse;
import com.swyp.picke.domain.user.dto.response.UserSummary;
import com.swyp.picke.domain.user.enums.PhilosopherType;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @Transactional(readOnly = true)
    public User findByUserTag(String userTag) {
        if (userTag == null || userTag.isBlank()) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        return userRepository.findByUserTag(userTag)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
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
        String characterType = profile.getCharacterType() != null ? profile.getCharacterType().name() : null;
        return new UserSummary(user.getUserTag(), profile.getNickname(), characterType);
    }

    public User findCurrentUser() {
        // 1. SecurityContext에서 현재 로그인한 유저의 인증 정보(토큰 정보)를 가져옵니다.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 2. 인증 정보가 없거나 비로그인 상태면 에러를 던집니다.
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND); // 또는 권한 없음(UNAUTHORIZED) 에러 코드로 변경하셔도 좋습니다.
        }

        // 3. Principal 객체에서 userId를 추출합니다. (컨트롤러에서 @AuthenticationPrincipal Long userId 로 받던 것과 같은 원리)
        Long userId = Long.valueOf(authentication.getPrincipal().toString());

        // 4. 진짜 '내' ID로 DB에서 유저를 조회해서 반환합니다!
        return userRepository.findById(userId)
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

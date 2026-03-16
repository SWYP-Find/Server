package com.swyp.app.domain.user.service;

import com.swyp.app.domain.user.entity.User;
import com.swyp.app.domain.user.entity.UserProfile;
import com.swyp.app.domain.user.repository.UserProfileRepository;
import com.swyp.app.domain.user.repository.UserRepository;
import com.swyp.app.global.common.exception.CustomException;
import com.swyp.app.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserQueryServiceImpl implements UserQueryService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    @Override
    public UserSummary findSummaryById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return new UserSummary(user.getUserTag(), profile.getNickname(), profile.getCharacterType().name());
    }
}

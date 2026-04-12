package com.swyp.picke.domain.share.service;

import com.swyp.picke.domain.share.dto.response.RecapShareKeyResponse;
import com.swyp.picke.domain.user.dto.response.RecapResponse;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.entity.UserProfile;
import com.swyp.picke.domain.user.repository.UserProfileRepository;
import com.swyp.picke.domain.user.service.MypageService;
import com.swyp.picke.domain.user.service.UserService;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShareService {

    private final UserService userService;
    private final UserProfileRepository userProfileRepository;
    private final MypageService mypageService;

    @Transactional
    public RecapShareKeyResponse getRecapShareKey() {
        User user = userService.findCurrentUser();
        UserProfile profile = userService.findUserProfile(user.getId());

        ensureRecapExists(user.getId());

        if (profile.getRecapShareKey() == null || profile.getRecapShareKey().isBlank()) {
            profile.updateRecapShareKey(UUID.randomUUID().toString());
        }

        return new RecapShareKeyResponse(profile.getRecapShareKey());
    }

    public RecapResponse getSharedRecap(String shareKey) {
        UserProfile profile = userProfileRepository.findByRecapShareKey(shareKey)
                .orElseThrow(() -> new CustomException(ErrorCode.RECAP_NOT_FOUND));

        RecapResponse recap = mypageService.findRecapByUserId(profile.getUser().getId());
        if (recap == null) {
            throw new CustomException(ErrorCode.RECAP_NOT_FOUND);
        }

        return recap;
    }

    private void ensureRecapExists(Long userId) {
        if (mypageService.findRecapByUserId(userId) == null) {
            throw new CustomException(ErrorCode.RECAP_NOT_FOUND);
        }
    }
}

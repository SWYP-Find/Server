package com.swyp.picke.domain.admin.service;

import com.swyp.picke.domain.admin.dto.user.response.AdminUserSearchResponse;
import com.swyp.picke.domain.admin.dto.user.response.AdminUserSummaryResponse;
import com.swyp.picke.domain.oauth.entity.UserSocialAccount;
import com.swyp.picke.domain.oauth.repository.UserSocialAccountRepository;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.repository.UserRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final UserRepository userRepository;
    private final UserSocialAccountRepository userSocialAccountRepository;

    public AdminUserSearchResponse searchUsers(String keyword, int page, int size) {
        int pageNumber = Math.max(0, page);
        int pageSize = size <= 0 ? DEFAULT_PAGE_SIZE : size;

        // 닉네임/유저태그(로컬+소셜 유저 공통)와 이메일(소셜 로그인 유저만 보유)은
        // 서로 다른 테이블에서 나오는 결과라 각각 조회 후 userId 기준으로 합친다.
        Slice<User> byNicknameOrTag = userRepository.searchByNicknameOrUserTag(
                keyword, PageRequest.of(pageNumber, pageSize));
        List<UserSocialAccount> byEmail = userSocialAccountRepository.findByProviderEmailContaining(keyword);

        Map<Long, User> merged = new LinkedHashMap<>();
        byNicknameOrTag.getContent().forEach(user -> merged.put(user.getId(), user));
        byEmail.forEach(socialAccount -> merged.put(socialAccount.getUser().getId(), socialAccount.getUser()));

        List<User> users = merged.values().stream()
                .sorted((a, b) -> Long.compare(b.getId(), a.getId()))
                .limit(pageSize)
                .toList();

        Map<Long, String> emailByUserId = emailByUserId(users);

        List<AdminUserSummaryResponse> items = users.stream()
                .map(user -> new AdminUserSummaryResponse(
                        user.getId(),
                        user.getUserTag(),
                        user.getNickname(),
                        emailByUserId.get(user.getId())
                ))
                .toList();

        boolean hasNext = byNicknameOrTag.hasNext() || merged.size() > pageSize;

        return new AdminUserSearchResponse(items, hasNext);
    }

    private Map<Long, String> emailByUserId(List<User> users) {
        List<Long> userIds = users.stream().map(User::getId).toList();
        Map<Long, String> emailByUserId = new LinkedHashMap<>();
        for (UserSocialAccount socialAccount : userSocialAccountRepository.findByUser_IdIn(userIds)) {
            emailByUserId.putIfAbsent(socialAccount.getUser().getId(), socialAccount.getProviderEmail());
        }
        return emailByUserId;
    }
}

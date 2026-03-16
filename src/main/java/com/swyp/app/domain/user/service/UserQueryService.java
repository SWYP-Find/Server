package com.swyp.app.domain.user.service;

public interface UserQueryService {

    UserSummary findSummaryById(Long userId);

    record UserSummary(String userTag, String nickname, String characterType) {}
}

package com.swyp.app.domain.user.service;

import org.springframework.stereotype.Service;

@Service
public class UserQueryServiceImpl implements UserQueryService {

    @Override
    public UserSummary findSummaryById(Long userId) {
        throw new UnsupportedOperationException("Not yet implemented - pending User domain merge");
    }
}

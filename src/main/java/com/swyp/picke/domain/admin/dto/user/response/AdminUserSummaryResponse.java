package com.swyp.picke.domain.admin.dto.user.response;

public record AdminUserSummaryResponse(
        Long userId,
        String userTag,
        String nickname,
        String email
) {}

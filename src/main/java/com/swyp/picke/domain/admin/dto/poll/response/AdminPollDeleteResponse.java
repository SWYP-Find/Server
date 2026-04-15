package com.swyp.picke.domain.admin.dto.poll.response;

import java.time.LocalDateTime;

public record AdminPollDeleteResponse(
        boolean success,
        LocalDateTime deletedAt
) {
}



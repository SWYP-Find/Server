package com.swyp.picke.domain.admin.dto.tag.response;

import java.time.LocalDateTime;

public record TagDeleteResponse(
        boolean success,
        LocalDateTime deletedAt
) {}
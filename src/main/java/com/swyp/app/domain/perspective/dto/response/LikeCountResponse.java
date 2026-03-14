package com.swyp.app.domain.perspective.dto.response;

import java.util.UUID;

public record LikeCountResponse(UUID perspectiveId, int likeCount) {}

package com.swyp.app.domain.perspective.dto.response;

import java.util.UUID;

public record LikeResponse(UUID perspectiveId, int likeCount, boolean isLiked) {}

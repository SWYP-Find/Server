package com.swyp.app.domain.tag.dto.response;

import java.util.List;

public record TagListResponse(
        List<TagResponse> items,
        int totalCount
) {}
package com.swyp.picke.domain.tag.dto.response;

import java.util.List;

public record TagListResponse(
        List<TagResponse> items,
        int totalCount
) {}
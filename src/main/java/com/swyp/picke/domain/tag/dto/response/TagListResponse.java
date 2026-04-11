package com.swyp.picke.domain.tag.dto.response;

import com.swyp.picke.domain.admin.dto.tag.response.TagResponse;
import java.util.List;

public record TagListResponse(
        List<TagResponse> items,
        int totalCount
) {}
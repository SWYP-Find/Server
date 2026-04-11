package com.swyp.picke.domain.poll.dto.response;

import java.util.List;

public record PollListResponse(
        List<PollSimpleResponse> items,
        int page,
        int totalPages,
        long totalElements
) {
}



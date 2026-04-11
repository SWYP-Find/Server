package com.swyp.picke.domain.poll.dto.response;

import com.swyp.picke.domain.poll.enums.PollOptionLabel;

public record PollOptionResponse(
        Long optionId,
        PollOptionLabel label,
        String title,
        Integer displayOrder,
        Long voteCount
) {
}



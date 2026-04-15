package com.swyp.picke.domain.poll.dto.response;

import com.swyp.picke.domain.poll.enums.PollStatus;

import java.time.LocalDateTime;

public record PollSimpleResponse(
        Long pollId,
        String titlePrefix,
        String titleSuffix,
        PollStatus status
) {
}



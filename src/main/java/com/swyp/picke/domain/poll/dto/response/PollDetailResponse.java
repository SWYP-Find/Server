package com.swyp.picke.domain.poll.dto.response;

import com.swyp.picke.domain.poll.enums.PollStatus;
import java.time.LocalDate;
import java.util.List;

public record PollDetailResponse(
        Long pollId,
        String titlePrefix,
        String titleSuffix,
        LocalDate targetDate,
        PollStatus status,
        List<PollOptionResponse> options
) {}

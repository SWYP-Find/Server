package com.swyp.picke.domain.admin.dto.poll.response;

import com.swyp.picke.domain.poll.dto.response.PollOptionResponse;
import com.swyp.picke.domain.poll.enums.PollStatus;

import java.time.LocalDate;
import java.util.List;

public record AdminPollDetailResponse(
        Long pollId,
        String titlePrefix,
        String titleSuffix,
        LocalDate targetDate,
        PollStatus status,
        List<PollOptionResponse> options
) {}
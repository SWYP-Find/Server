package com.swyp.picke.domain.admin.dto.poll.request;

import com.swyp.picke.domain.poll.enums.PollStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AdminPollCreateRequest(
        String titlePrefix,
        String titleSuffix,
        LocalDate targetDate,
        LocalDateTime publishAt,
        PollStatus status,
        List<AdminPollOptionRequest> options
) {
}



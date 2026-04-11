package com.swyp.picke.domain.admin.dto.poll.request;

import com.swyp.picke.domain.poll.enums.PollStatus;
import java.util.List;

public record AdminPollCreateRequest(
        String titlePrefix,
        String titleSuffix,
        PollStatus status,
        List<AdminPollOptionRequest> options
) {
}



package com.swyp.picke.domain.admin.dto.poll.request;

import com.swyp.picke.domain.poll.enums.PollOptionLabel;

public record AdminPollOptionRequest(
        PollOptionLabel label,
        String title
) {}



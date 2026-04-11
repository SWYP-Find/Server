package com.swyp.picke.domain.poll.dto.response;

import com.swyp.picke.domain.tag.enums.TagType;

public record PollTagResponse(
        Long tagId,
        String name,
        TagType type
) {
}



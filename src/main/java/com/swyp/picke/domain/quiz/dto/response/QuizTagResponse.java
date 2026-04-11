package com.swyp.picke.domain.quiz.dto.response;

import com.swyp.picke.domain.tag.enums.TagType;

public record QuizTagResponse(
        Long tagId,
        String name,
        TagType type
) {
}



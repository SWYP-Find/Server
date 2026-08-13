package com.swyp.picke.domain.user.enums;

import java.util.Arrays;
import java.util.Optional;

public enum ValueAxis {
    PRINCIPLE("원칙", "결과"),
    REASON("이성", "감성"),
    INDIVIDUAL("개인", "관계"),
    CHANGE("변화", "전통"),
    INNER("내면", "구조"),
    IDEAL("이상", "현실");

    private final String positiveTag;
    private final String negativeTag;

    ValueAxis(String positiveTag, String negativeTag) {
        this.positiveTag = positiveTag;
        this.negativeTag = negativeTag;
    }

    public static Optional<ValueDirection> resolve(String tagName) {
        return Arrays.stream(values())
                .filter(axis -> axis.positiveTag.equals(tagName) || axis.negativeTag.equals(tagName))
                .findFirst()
                .map(axis -> new ValueDirection(
                        axis,
                        axis.positiveTag.equals(tagName) ? 1 : -1
                ));
    }

    public record ValueDirection(ValueAxis axis, int direction) {
    }
}

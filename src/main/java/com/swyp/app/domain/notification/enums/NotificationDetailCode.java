package com.swyp.app.domain.notification.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationDetailCode {

    // CONTENT (1~3)
    NEW_BATTLE(1, NotificationCategory.CONTENT, "새로운 배틀이 시작되었어요"),
    VOTE_RESULT(2, NotificationCategory.CONTENT, "투표 결과가 나왔어요"),
    CREDIT_EARNED(3, NotificationCategory.CONTENT, "포인트 적립"),

    // NOTICE (4)
    POLICY_CHANGE(4, NotificationCategory.NOTICE, "공지사항"),

    // EVENT (5)
    PROMOTION(5, NotificationCategory.EVENT, "이벤트");

    private final int code;
    private final NotificationCategory category;
    private final String defaultTitle;
}

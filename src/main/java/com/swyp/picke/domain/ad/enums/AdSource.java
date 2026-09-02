package com.swyp.picke.domain.ad.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 소재를 누가 만들었는지.
 * ADPICK_API 소재는 동기화가 내용을 덮어쓰므로 어드민에서 수정·삭제하지 않는다.
 */
@Getter
@RequiredArgsConstructor
public enum AdSource {

    MANUAL("어드민 수동 등록"),
    ADPICK_API("애드픽 캠페인 API 동기화");

    private final String description;
}

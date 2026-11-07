package com.swyp.picke.domain.battle.thumbnail;

/** OpenAI 안전 필터(moderation_blocked)가 이미지 생성 요청을 거절했다. */
public class ImageModerationBlockedException extends RuntimeException {

    public ImageModerationBlockedException(String message, Throwable cause) {
        super(message, cause);
    }
}

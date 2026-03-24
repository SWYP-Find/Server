package com.swyp.app.global.infra.s3.enums;

import lombok.Getter;

@Getter
public enum FileCategory {

    PHILOSOPHER("images/philosophers"), // 철학자 이미지
    BATTLE("images/battles"),           // 배틀 썸네일
    SCENARIO("audio/scenarios");        // 시나리오 음성

    private final String path;

    FileCategory(String path) {
        this.path = path;
    }
}
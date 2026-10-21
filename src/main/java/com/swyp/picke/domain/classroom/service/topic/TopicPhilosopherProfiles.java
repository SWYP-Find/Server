package com.swyp.picke.domain.classroom.service.topic;

import com.swyp.picke.domain.user.enums.PhilosopherType;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 주제 선택지에 매핑할 수 있는 철학자 허용 목록 (PRD §10 option.profile_id).
 * 서비스에 이미 있는 {@link PhilosopherType} 10명을 그대로 쓰고, 목록이나 설명이 바뀌면 PROFILE_VERSION 을 올린다.
 */
public final class TopicPhilosopherProfiles {

    public static final String PROFILE_VERSION = "philosopher-type-v1";

    private TopicPhilosopherProfiles() {
    }

    /** 목록에 없는 ID 면 null. 임의 인물을 만들지 않도록 이름 대신 enum 코드만 받는다 (A03) */
    public static PhilosopherType parse(String id) {
        if (id == null) {
            return null;
        }
        String normalized = id.strip();
        return Arrays.stream(PhilosopherType.values())
                .filter(type -> type.name().equals(normalized))
                .findFirst()
                .orElse(null);
    }

    static String promptList() {
        return Arrays.stream(PhilosopherType.values())
                .map(type -> "- " + type.name() + ": " + type.getLabel() + " · " + type.getTypeName()
                        + " · " + type.getDescription() + " · " + String.join(" ", type.getKeywordTags()))
                .collect(Collectors.joining("\n"));
    }
}

package com.swyp.picke.domain.user.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

@Getter
public enum CharacterType {
    OWL("부엉이", "images/characters/owl.png"),
    FOX("여우", "images/characters/fox.png"),
    WOLF("늑대", "images/characters/wolf.png"),
    LION("사자", "images/characters/lion.png"),
    PENGUIN("펭귄", "images/characters/penguin.png"),
    BEAR("곰", "images/characters/bear.png"),
    RABBIT("토끼", "images/characters/rabbit.png"),
    CAT("고양이", "images/characters/cat.png"),
    ALPACA("알파카", "images/characters/alpaca.png"),
    CAPYBARA("카피바라", "images/characters/capybara.png"),
    DEER("사슴", "images/characters/deer.png"),
    DOG("강아지", "images/characters/dog.png"),
    DUCK("오리", "images/characters/duck.png"),
    EAGLE("독수리", "images/characters/eagle.png"),
    HAMSTER("햄스터", "images/characters/hamster.png"),
    HEDGEHOG("고슴도치", "images/characters/hedgehog.png"),
    HONEYBEE("꿀벌", "images/characters/honeybee.png"),
    KOALA("코알라", "images/characters/koala.png"),
    OTTER("수달", "images/characters/otter.png"),
    PANDA("판다", "images/characters/panda.png"),
    POODLE("푸들", "images/characters/poodle.png"),
    RACCOON("라쿤", "images/characters/raccoon.png"),
    RAGDOLL("랙돌", "images/characters/ragdoll.png"),
    RETRIEVER("리트리버", "images/characters/retriever.png"),
    SLOTH("나무늘보", "images/characters/sloth.png"),
    SQUIRREL("다람쥐", "images/characters/squirrel.png"),
    TIGER("호랑이", "images/characters/tiger.png"),
    WHALE("고래", "images/characters/whale.png");

    private static final CharacterType[] VALUES = values();

    private final String label;
    private final String imageKey;

    CharacterType(String label, String imageKey) {
        this.label = label;
        this.imageKey = imageKey;
    }

    public static CharacterType from(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Unknown character type: " + input);
        }
        String normalizedInput = input.trim();
        return Arrays.stream(VALUES)
                .filter(type -> type.name().equalsIgnoreCase(normalizedInput) || type.label.equals(normalizedInput))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown character type: " + input));
    }

    public static CharacterType random() {
        return VALUES[ThreadLocalRandom.current().nextInt(VALUES.length)];
    }

    public static String resolveImageKey(String input) {
        return from(input).getImageKey();
    }

    public static String resolveImageKey(CharacterType type) {
        return type == null ? null : type.getImageKey();
    }
}

package com.swyp.picke.domain.user.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum CharacterType {
    OWL("부엉이", "images/characters/owl.png"),
    FOX("여우", "images/characters/fox.png"),
    WOLF("늑대", "images/characters/wolf.png"),
    LION("사자", "images/characters/lion.png"),
    PENGUIN("펭귄", "images/characters/penguin.png"),
    BEAR("곰", "images/characters/bear.png"),
    RABBIT("토끼", "images/characters/rabbit.png"),
    CAT("고양이", "images/characters/cat.png");

    private final String label;
    private final String imageKey;

    CharacterType(String label, String imageKey) {
        this.label = label;
        this.imageKey = imageKey;
    }

    public static CharacterType from(String input) {
        return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(input) || type.label.equals(input))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown character type: " + input));
    }
}

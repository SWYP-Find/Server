package com.swyp.app.domain.user.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum CharacterType {
    OWL("owl"),
    FOX("fox"),
    WOLF("wolf"),
    LION("lion"),
    PENGUIN("penguin"),
    BEAR("bear"),
    RABBIT("rabbit"),
    CAT("cat");

    private final String value;

    CharacterType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static CharacterType from(String value) {
        return Arrays.stream(values())
                .filter(type -> type.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown character type: " + value));
    }
}

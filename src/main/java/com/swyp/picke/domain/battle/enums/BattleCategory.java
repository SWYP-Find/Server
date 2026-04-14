package com.swyp.picke.domain.battle.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum BattleCategory {
    PHILOSOPHY("철학"),
    LITERATURE("문학"),
    ART("예술"),
    SCIENCE("과학"),
    SOCIETY("사회"),
    HISTORY("역사");

    private final String value;
    BattleCategory(String value) { this.value = value; }

    @JsonCreator
    public static BattleCategory from(String value) {
        for (BattleCategory category : BattleCategory.values()) {
            if (category.value.equals(value)) {
                return category;
            }
        }
        return null;
    }

    @JsonValue
    public String getValue() { return value; }
}
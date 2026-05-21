package com.swyp.picke.domain.battle.util;

import com.swyp.picke.domain.battle.entity.BattleOption;

public final class BattleOptionDisplay {

    private BattleOptionDisplay() {
    }

    public static String opinion(BattleOption option) {
        if (option == null) {
            return null;
        }
        String title = option.getTitle();
        if (title != null && !title.isBlank()) {
            return title;
        }
        return option.getLabel() == null ? null : option.getLabel().name();
    }
}

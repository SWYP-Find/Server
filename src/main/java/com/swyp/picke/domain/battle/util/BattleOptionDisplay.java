package com.swyp.picke.domain.battle.util;

import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.user.enums.VoteSide;

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
        return null;
    }

    public static VoteSide voteSide(BattleOption option) {
        if (option == null || option.getDisplayOrder() == null) {
            return null;
        }
        return option.getDisplayOrder() == 1 ? VoteSide.PRO : VoteSide.CON;
    }
}

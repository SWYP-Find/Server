package com.swyp.picke.domain.classroom.service.topic;

import com.swyp.picke.domain.battle.enums.BattleCategory;
import com.swyp.picke.domain.battle.enums.BattleDifficulty;
import java.util.List;

/** 주제 제안 조건 (PRD §5): 키워드 2~200자, 수준, 카테고리 1~3개 */
public record TopicConditions(String topicSeed, BattleDifficulty difficulty, List<BattleCategory> categories) {

    public List<String> categoryNames() {
        return categories.stream().map(Enum::name).toList();
    }
}

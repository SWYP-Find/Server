package com.swyp.picke.domain.classroom.service.topic;

import com.swyp.picke.domain.battle.enums.BattleCategory;
import com.swyp.picke.domain.battle.enums.BattleDifficulty;
import com.swyp.picke.domain.user.enums.PhilosopherType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 주제 한 건(질문·설명·선택지 2개). AI 후보와 운영자 수정안이 같은 형태를 쓴다.
 * 후보는 class_topic_drafts.candidates 에 {@link #toMap(int)} 형태로 저장한다.
 */
public record TopicContent(
        String question,
        String description,
        BattleCategory category,
        BattleDifficulty difficulty,
        Option optionA,
        Option optionB
) {

    public record Option(String label, String rationale, PhilosopherType philosopher) {
    }

    public List<Option> options() {
        return List.of(optionA, optionB);
    }

    public Map<String, Object> toMap(int candidateId) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("candidate_id", candidateId);
        map.put("question", question);
        map.put("description", description);
        map.put("category", category.name());
        map.put("difficulty", difficulty.name());
        map.put("options", List.of(optionMap(optionA), optionMap(optionB)));
        return map;
    }

    @SuppressWarnings("unchecked")
    public static TopicContent fromMap(Map<String, Object> map) {
        List<Map<String, Object>> options = (List<Map<String, Object>>) map.get("options");
        return new TopicContent(
                (String) map.get("question"),
                (String) map.get("description"),
                BattleCategory.valueOf((String) map.get("category")),
                BattleDifficulty.valueOf((String) map.get("difficulty")),
                optionOf(options.get(0)),
                optionOf(options.get(1)));
    }

    public static int candidateIdOf(Map<String, Object> map) {
        return ((Number) map.get("candidate_id")).intValue();
    }

    private static Map<String, Object> optionMap(Option option) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("label", option.label());
        map.put("rationale", option.rationale());
        map.put("philosopher", option.philosopher().name());
        return map;
    }

    private static Option optionOf(Map<String, Object> map) {
        return new Option(
                (String) map.get("label"),
                (String) map.get("rationale"),
                PhilosopherType.valueOf((String) map.get("philosopher")));
    }
}

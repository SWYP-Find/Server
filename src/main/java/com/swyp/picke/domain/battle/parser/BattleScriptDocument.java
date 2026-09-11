package com.swyp.picke.domain.battle.parser;

import com.swyp.picke.domain.battle.dto.parse.ParseWarning;
import java.util.ArrayList;
import java.util.List;

/**
 * 붙여넣은 대본을 정규식으로 1차 분해한 중간 표현. 태그 ID·보이스·감정 분류는 아직 안 붙었다.
 */
public class BattleScriptDocument {

    public boolean interactive;
    public String title;
    public String category;
    public OptionMeta optionA = new OptionMeta();
    public OptionMeta optionB = new OptionMeta();
    public final List<ParsedNode> nodes = new ArrayList<>();
    public final List<ParseWarning> warnings = new ArrayList<>();

    public static class OptionMeta {
        public String choiceName;
        /** 사전 투표 줄에 적힌 대표 발화자(예: "플라톤"). A/B 바인딩 힌트로 쓴다. */
        public String primarySpeaker;
        public final List<String> philosopherKeywords = new ArrayList<>();
        public final List<String> valueTags = new ArrayList<>();
    }

    public static class ParsedNode {
        public String name;
        public final List<ParsedScript> scripts = new ArrayList<>();
        /** 선택의 시간 노드에서만: label(A/B) -> 다음 노드 이름 */
        public final List<ParsedOption> options = new ArrayList<>();

        public ParsedNode(String name) {
            this.name = name;
        }
    }

    public static class ParsedScript {
        /** 철학자 이름. null 이면 나레이터. */
        public String speaker;
        public String text;

        public ParsedScript(String speaker, String text) {
            this.speaker = speaker;
            this.text = text;
        }
    }

    public static class ParsedOption {
        public String label;        // "A" / "B"
        public String nextNodeName; // "분기_A" 등
        public String speaker;      // 선택의 시간 줄에 표기된 발화자 (예: "칸트")

        public ParsedOption(String label, String nextNodeName, String speaker) {
            this.label = label;
            this.nextNodeName = nextNodeName;
            this.speaker = speaker;
        }
    }
}

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
        /** 사전 투표 줄의 짧은 선택지명(예: "유죄다"). 있으면 이게 최종 title 이 된다. */
        public String choiceName;
        /** 메타데이터 표 "선택지 명칭" 값. 사전 투표가 없을 때만 title 로 쓰는 폴백. */
        public String metadataChoiceName;
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
        public String title;        // 선택의 시간 줄의 설명 텍스트 (예: "첫 만남에는 그에 걸맞은 격조와 분위기가 필수다, 유죄!")
        public String nextNodeName; // "분기_A" 등
        public String speaker;      // 선택의 시간 줄에 표기된 발화자 (예: "칸트")

        public ParsedOption(String label, String title, String nextNodeName, String speaker) {
            this.label = label;
            this.title = title;
            this.nextNodeName = nextNodeName;
            this.speaker = speaker;
        }
    }
}

package com.swyp.picke.domain.battle.parser;

import static org.assertj.core.api.Assertions.assertThat;

import com.swyp.picke.domain.battle.parser.BattleScriptDocument.ParsedNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class BattleScriptDocumentParserTest {

    private final BattleScriptDocumentParser parser = new BattleScriptDocumentParser();

    private String load(String name) throws IOException {
        return new String(new ClassPathResource("battle-scripts/" + name).getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);
    }

    private ParsedNode node(BattleScriptDocument doc, String name) {
        return doc.nodes.stream().filter(n -> n.name.equals(name)).findFirst().orElse(null);
    }

    @Test
    void 선형_대본_헤더와_메타데이터를_파싱한다() throws IOException {
        BattleScriptDocument doc = parser.parse(load("sample-linear.txt"));

        assertThat(doc.interactive).isFalse();
        assertThat(doc.title).isEqualTo("인간은 본래 선한가, 악한가?");
        assertThat(doc.category).isEqualTo("철학");
        assertThat(doc.optionA.choiceName).isEqualTo("선하다");
        assertThat(doc.optionA.philosopherKeywords).containsExactly("노자", "플라톤", "석가모니");
        assertThat(doc.optionA.valueTags).containsExactly("내면", "이상");
        assertThat(doc.optionB.choiceName).isEqualTo("악하다");
        assertThat(doc.optionB.valueTags).containsExactly("원칙", "이성");
    }

    @Test
    void 선형_대본은_오프닝_라운드_클로징으로_나뉜다() throws IOException {
        BattleScriptDocument doc = parser.parse(load("sample-linear.txt"));

        assertThat(doc.nodes).extracting(n -> n.name).containsExactly("오프닝", "라운드", "클로징");
        assertThat(node(doc, "오프닝").scripts).allMatch(s -> s.speaker == null);
        assertThat(node(doc, "라운드").scripts).anyMatch(s -> "루소".equals(s.speaker));
        assertThat(node(doc, "라운드").scripts).anyMatch(s -> "홉스".equals(s.speaker));
        assertThat(node(doc, "클로징").scripts).hasSize(1);
        assertThat(node(doc, "클로징").scripts.get(0).text).startsWith("루소는 말합니다");
    }

    @Test
    void 인터랙티브_대본은_분기_버전을_노드_원본으로_쓴다() throws IOException {
        BattleScriptDocument doc = parser.parse(load("sample-interactive.txt"));

        assertThat(doc.interactive).isTrue();
        assertThat(doc.title).isEqualTo("AI 판사는 도입해야 하는가?");
        assertThat(doc.nodes).extracting(n -> n.name)
                .containsExactly("오프닝", "1라운드", "2라운드", "선택", "분기_A", "분기_B", "클로징");
    }

    @Test
    void 선택의_시간_노드는_A_B_옵션과_발화자를_담는다() throws IOException {
        BattleScriptDocument doc = parser.parse(load("sample-interactive.txt"));
        ParsedNode choice = node(doc, "선택");

        assertThat(choice.options).hasSize(2);
        assertThat(choice.options).extracting(o -> o.label).containsExactly("A", "B");
        assertThat(choice.options).extracting(o -> o.nextNodeName).containsExactly("분기_A", "분기_B");
        assertThat(choice.options).extracting(o -> o.speaker).containsExactly("칸트", "아리스토텔레스");
    }

    @Test
    void 라운드_노드의_발화자_줄을_화자와_대사로_분리한다() throws IOException {
        BattleScriptDocument doc = parser.parse(load("sample-interactive.txt"));
        ParsedNode round1 = node(doc, "1라운드");

        assertThat(round1.scripts).hasSize(2);
        assertThat(round1.scripts.get(0).speaker).isEqualTo("칸트");
        assertThat(round1.scripts.get(0).text).startsWith("정의는 보편적 원칙");
        assertThat(round1.scripts.get(1).speaker).isEqualTo("아리스토텔레스");
    }

    @Test
    void 인터랙티브_메타데이터도_양쪽_옵션을_파싱한다() throws IOException {
        BattleScriptDocument doc = parser.parse(load("sample-interactive.txt"));

        assertThat(doc.optionA.choiceName).isEqualTo("AI 판사를 도입해야 한다");
        assertThat(doc.optionA.philosopherKeywords).containsExactly("칸트", "플라톤", "소크라테스");
        assertThat(doc.optionB.philosopherKeywords).containsExactly("아리스토텔레스", "공자", "사르트르");
        assertThat(doc.optionB.valueTags).containsExactly("개인", "변화");
    }
}

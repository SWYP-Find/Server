package com.swyp.picke.domain.battle.parser;

import com.swyp.picke.domain.battle.dto.parse.ParseWarning;
import com.swyp.picke.domain.battle.parser.BattleScriptDocument.OptionMeta;
import com.swyp.picke.domain.battle.parser.BattleScriptDocument.ParsedNode;
import com.swyp.picke.domain.battle.parser.BattleScriptDocument.ParsedOption;
import com.swyp.picke.domain.battle.parser.BattleScriptDocument.ParsedScript;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * 붙여넣은 대본 텍스트를 정규식으로 {@link BattleScriptDocument} 로 분해한다.
 * 외부 의존성 없음(태그·보이스·LLM 은 상위 서비스가 붙인다). 포맷 규격: 배틀_발행_시나리오_현행_vs_개선.md 2.6
 */
@Component
public class BattleScriptDocumentParser {

    private static final Pattern HEADER =
            Pattern.compile("^\\s*(?:분기\\s*[OX]\\s*버전\\s*)?\\S+\\s*[—–-]\\s*(.+)$");
    private static final Pattern INTERACTIVE_HINT = Pattern.compile("분기\\s*([OX])");
    private static final Pattern PREVOTE = Pattern.compile(
            "[\"'“]?\\s*([^\"'”()]+?)\\s*\\(([^)]+)\\)\\s*[\"'”]?\\s*(?:vs|VS|대)\\s*"
                    + "[\"'“]?\\s*([^\"'”()]+?)\\s*\\(([^)]+)\\)");
    private static final Pattern SECTION_OPENING = Pattern.compile("^\\[?\\s*오프닝\\s*\\]?\\s*[:：]?.*$");
    private static final Pattern SECTION_ROUND = Pattern.compile("^\\[?\\s*(\\d+)\\s*라운드\\s*\\]?\\s*[:：]?.*$");
    private static final Pattern SECTION_CLOSING = Pattern.compile("^\\[?\\s*클로징\\s*\\]?\\s*[:：]?.*$");
    private static final Pattern SECTION_CHOICE = Pattern.compile(".*선택의\\s*시간.*");
    private static final Pattern OPTION_LINE = Pattern.compile("^\\s*([ABab])\\s*[:：]\\s*(.+)$");
    private static final Pattern SECTION_BRANCH = Pattern.compile("^\\[\\s*분기\\s*([AB])\\b[^\\]]*\\]\\s*$");
    private static final Pattern INLINE_BRANCH = Pattern.compile("^\\[\\s*([AB])\\s*선택\\s*시\\s*\\]\\s*(.*)$");
    private static final Pattern BRANCH_CONTAINER = Pattern.compile("^\\[\\s*분기\\s*시나리오[^\\]]*\\]\\s*$");
    private static final Pattern SPEAKER_LINE = Pattern.compile("^([^:：]{1,25})[:：]\\s*(.+)$");
    private static final Pattern SPEAKER_ANNOTATION = Pattern.compile("\\s*\\([^)]*\\)\\s*$");
    private static final Pattern TRAILING_SPEAKER = Pattern.compile("\\(([^)]+)\\)\\s*$");
    private static final Pattern VALUE_TAG = Pattern.compile("\\[([^\\]]+)\\]");
    private static final Pattern BRANCH_MARKER = Pattern.compile("^\\(?\\s*분기\\s*버전\\s*\\)?$");
    private static final Pattern METADATA_MARKER = Pattern.compile("^\\s*메타데이터\\s*$");
    private static final Pattern PREVOTE_MARKER = Pattern.compile("^\\s*사전\\s*투표\\s*$");

    public BattleScriptDocument parse(String rawText) {
        BattleScriptDocument doc = new BattleScriptDocument();
        List<String> lines = normalizeLines(rawText);

        int headerIdx = indexOfHeader(lines, doc);
        int metadataIdx = firstMatch(lines, METADATA_MARKER, headerIdx + 1);
        int branchIdx = firstMatch(lines, BRANCH_MARKER, headerIdx + 1);

        parsePreVote(lines, headerIdx, metadataIdx, doc);

        List<String> bodyLines;
        if (branchIdx >= 0) {
            bodyLines = lines.subList(branchIdx + 1, lines.size());
        } else {
            int bodyEnd = metadataIdx >= 0 ? metadataIdx : lines.size();
            bodyLines = lines.subList(headerIdx + 1, bodyEnd);
        }
        parseBody(bodyLines, doc);

        int metaEnd = branchIdx >= 0 && branchIdx > metadataIdx ? branchIdx : lines.size();
        if (metadataIdx >= 0) {
            parseMetadata(lines.subList(metadataIdx + 1, metaEnd), doc);
        } else {
            doc.warnings.add(ParseWarning.of("MISSING_METADATA",
                    "메타데이터 섹션을 찾지 못했습니다. 카테고리/선택지/태그를 미리보기에서 직접 입력하세요.", null));
        }

        // 본문에 선택/분기가 있으면 인터랙티브로 승격
        boolean bodyIsInteractive = doc.nodes.stream()
                .anyMatch(n -> n.name.equals("선택") || n.name.startsWith("분기_"));
        doc.interactive = doc.interactive || bodyIsInteractive;
        return doc;
    }

    private List<String> normalizeLines(String rawText) {
        List<String> out = new ArrayList<>();
        if (rawText == null) {
            return out;
        }
        for (String line : rawText.replace("\r\n", "\n").replace("\r", "\n").split("\n")) {
            String trimmed = line.strip();
            if (!trimmed.isEmpty()) {
                out.add(trimmed);
            }
        }
        return out;
    }

    private int indexOfHeader(List<String> lines, BattleScriptDocument doc) {
        for (int i = 0; i < lines.size(); i++) {
            Matcher m = HEADER.matcher(lines.get(i));
            if (m.matches()) {
                Matcher hint = INTERACTIVE_HINT.matcher(lines.get(i));
                doc.interactive = hint.find() && "O".equalsIgnoreCase(hint.group(1));
                doc.title = m.group(1).strip();
                return i;
            }
        }
        doc.interactive = false;
        doc.title = "(제목 미상)";
        doc.warnings.add(ParseWarning.of("MISSING_HEADER",
                "'제목' 형식 헤더(맨 윗줄, '— 제목')를 찾지 못했습니다. 제목을 확인하세요.", null));
        return -1;
    }

    private void parsePreVote(List<String> lines, int headerIdx, int metadataIdx, BattleScriptDocument doc) {
        int end = metadataIdx >= 0 ? metadataIdx : lines.size();
        for (int i = Math.max(headerIdx, 0); i < end; i++) {
            if (PREVOTE_MARKER.matcher(lines.get(i)).matches() || lines.get(i).contains("사전 투표")) {
                for (int j = i; j < Math.min(i + 3, end); j++) {
                    Matcher m = PREVOTE.matcher(lines.get(j));
                    if (m.find()) {
                        doc.optionA.choiceName = m.group(1).strip();
                        doc.optionA.primarySpeaker = m.group(2).strip();
                        doc.optionB.choiceName = m.group(3).strip();
                        doc.optionB.primarySpeaker = m.group(4).strip();
                        return;
                    }
                }
            }
        }
    }

    private int firstMatch(List<String> lines, Pattern pattern, int from) {
        for (int i = Math.max(from, 0); i < lines.size(); i++) {
            if (pattern.matcher(lines.get(i)).matches()) {
                return i;
            }
        }
        return -1;
    }

    private void parseBody(List<String> body, BattleScriptDocument doc) {
        ParsedNode current = new ParsedNode("오프닝");

        for (String line : body) {
            if (PREVOTE_MARKER.matcher(line).matches() || PREVOTE.matcher(line).find()
                    || BRANCH_CONTAINER.matcher(line).matches()) {
                continue; // 사전 투표 / 분기 컨테이너 헤더는 본문에서 무시
            }

            Matcher round = SECTION_ROUND.matcher(line);
            Matcher branch = SECTION_BRANCH.matcher(line);
            Matcher inlineBranch = INLINE_BRANCH.matcher(line);

            if (SECTION_OPENING.matcher(line).matches()) {
                current = pushAndStart(doc, current, "오프닝");
                continue;
            }
            if (SECTION_CHOICE.matcher(line).matches() && !OPTION_LINE.matcher(line).matches()) {
                current = pushAndStart(doc, current, "선택");
                continue;
            }
            if (round.matches()) {
                current = pushAndStart(doc, current, round.group(1) + "라운드");
                continue;
            }
            if (branch.matches()) {
                current = pushAndStart(doc, current, "분기_" + branch.group(1).toUpperCase());
                continue;
            }
            if (inlineBranch.matches()) {
                current = pushAndStart(doc, current, "분기_" + inlineBranch.group(1).toUpperCase());
                addDialogueLine(current, inlineBranch.group(2).strip());
                continue;
            }
            if (SECTION_CLOSING.matcher(line).matches()) {
                current = pushAndStart(doc, current, "클로징");
                continue;
            }

            if ("선택".equals(current.name)) {
                parseChoiceLine(line, current);
                continue;
            }
            addDialogueLine(current, line);
        }
        pushIfNotEmpty(doc, current);
        splitFlatBody(doc);
        if (doc.nodes.isEmpty()) {
            doc.warnings.add(ParseWarning.of("EMPTY_BODY", "본문에서 대사를 찾지 못했습니다.", null));
        }
    }

    private void addDialogueLine(ParsedNode node, String line) {
        if (line.isEmpty()) {
            return;
        }
        Matcher speaker = SPEAKER_LINE.matcher(line);
        if (speaker.matches() && !isSentenceColon(speaker.group(1))) {
            String name = SPEAKER_ANNOTATION.matcher(speaker.group(1).strip()).replaceAll("").strip();
            node.scripts.add(new ParsedScript(name, stripQuotes(speaker.group(2).strip())));
        } else {
            node.scripts.add(new ParsedScript(null, stripQuotes(line)));
        }
    }

    private String stripQuotes(String text) {
        if (text.length() >= 2) {
            char first = text.charAt(0);
            char last = text.charAt(text.length() - 1);
            if ((first == '"' && last == '"') || (first == '“' && last == '”') || (first == '\'' && last == '\'')) {
                return text.substring(1, text.length() - 1).strip();
            }
        }
        return text;
    }

    /** 라운드/분기 헤더 없이 오프닝 하나에 대사가 다 들어간 플랫 본문: 첫 나레이션=오프닝, 마지막 나레이션=클로징, 나머지=라운드. */
    private void splitFlatBody(BattleScriptDocument doc) {
        if (doc.nodes.size() != 1 || !doc.nodes.get(0).name.equals("오프닝")) {
            return;
        }
        List<ParsedScript> all = doc.nodes.get(0).scripts;
        boolean hasSpeaker = all.stream().anyMatch(s -> s.speaker != null);
        if (!hasSpeaker) {
            return;
        }
        doc.nodes.clear();

        ParsedNode opening = new ParsedNode("오프닝");
        ParsedNode round = new ParsedNode("라운드");
        ParsedNode closing = new ParsedNode("클로징");

        int lastSpeaker = -1;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).speaker != null) {
                lastSpeaker = i;
            }
        }
        for (int i = 0; i < all.size(); i++) {
            ParsedScript s = all.get(i);
            if (round.scripts.isEmpty() && s.speaker == null) {
                opening.scripts.add(s);
            } else if (lastSpeaker >= 0 && i > lastSpeaker && s.speaker == null) {
                closing.scripts.add(s);
            } else {
                round.scripts.add(s);
            }
        }
        pushIfNotEmpty(doc, opening);
        pushIfNotEmpty(doc, round);
        pushIfNotEmpty(doc, closing);
    }

    private void parseChoiceLine(String line, ParsedNode choiceNode) {
        Matcher m = OPTION_LINE.matcher(line);
        if (!m.matches()) {
            choiceNode.scripts.add(new ParsedScript(null, line));
            return;
        }
        String label = m.group(1).strip().toUpperCase();
        String rest = m.group(2).strip();
        String speaker = null;
        Matcher t = TRAILING_SPEAKER.matcher(rest);
        if (t.find()) {
            speaker = t.group(1).strip();
        }
        choiceNode.options.add(new ParsedOption(label, "분기_" + label, speaker));
    }

    private void parseMetadata(List<String> meta, BattleScriptDocument doc) {
        OptionMeta target = null;
        for (int i = 0; i < meta.size(); i++) {
            String line = meta.get(i);
            String next = i + 1 < meta.size() ? meta.get(i + 1) : null;

            if (line.contains("A안")) {
                target = doc.optionA;
            } else if (line.contains("B안")) {
                target = doc.optionB;
            } else if (line.startsWith("카테고리")) {
                doc.category = valueAfter(line, "카테고리", next);
            } else if (line.startsWith("선택지 명칭") && target != null) {
                target.choiceName = valueAfter(line, "선택지 명칭", next);
            } else if (line.startsWith("철학자 키워드") && target != null) {
                for (String kw : valueAfter(line, "철학자 키워드", next).split("[,、\\s]+")) {
                    String cleaned = kw.replace("#", "").strip();
                    if (!cleaned.isEmpty()) {
                        target.philosopherKeywords.add(cleaned);
                    }
                }
            } else if (line.startsWith("성향 지표") && target != null) {
                Matcher vm = VALUE_TAG.matcher(valueAfter(line, "성향 지표", next));
                while (vm.find()) {
                    target.valueTags.add(vm.group(1).strip());
                }
            }
        }
        if (doc.category == null) {
            doc.warnings.add(ParseWarning.of("MISSING_CATEGORY", "메타데이터에서 카테고리를 찾지 못했습니다.", null));
        }
    }

    /** "카테고리: 철학" 처럼 같은 줄에 값이 있으면 그 값을, 없으면 다음 줄을 값으로 본다. */
    private String valueAfter(String line, String key, String nextLine) {
        String inline = line.substring(line.indexOf(key) + key.length())
                .replaceFirst("^\\s*[:：]?\\s*", "")
                .replaceAll("\\(.*?\\)", "")
                .strip();
        if (!inline.isEmpty()) {
            return inline;
        }
        return nextLine == null ? "" : nextLine.strip();
    }

    private boolean isSentenceColon(String prefix) {
        String p = prefix.strip();
        if (p.isBlank() || p.length() > 20) {
            return true;
        }
        if (p.startsWith("[") || p.startsWith("👉") || p.contains("라운드")) {
            return true;
        }
        String withoutAnnotation = SPEAKER_ANNOTATION.matcher(p).replaceAll("").strip();
        return withoutAnnotation.split("\\s+").length > 3;
    }

    private ParsedNode pushAndStart(BattleScriptDocument doc, ParsedNode current, String newName) {
        pushIfNotEmpty(doc, current);
        return new ParsedNode(newName);
    }

    private void pushIfNotEmpty(BattleScriptDocument doc, ParsedNode node) {
        if (!node.scripts.isEmpty() || !node.options.isEmpty()) {
            mergeOrAdd(doc, node);
        }
    }

    private void mergeOrAdd(BattleScriptDocument doc, ParsedNode node) {
        for (ParsedNode existing : doc.nodes) {
            if (existing.name.equals(node.name)) {
                existing.scripts.addAll(node.scripts);
                existing.options.addAll(node.options);
                return;
            }
        }
        doc.nodes.add(node);
    }
}

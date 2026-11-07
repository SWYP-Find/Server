package com.swyp.picke.domain.classroom.service.debate;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 주장 하나를 화면 말풍선 2~3개로 나눈다 (기획 「AI 철학자 프롬프트 설정」 말풍선 구성).
 * <ul>
 *   <li>모델이 줄바꿈으로 의미 단위를 나눠 주면 그대로 쓴다</li>
 *   <li>말풍선이 1개이거나 4개 이상이면 AI 를 다시 부르지 않고 문장 끝을 기준으로 길이가 비슷하게 다시 묶는다</li>
 *   <li>한 문장뿐이면 가운데에 가까운 쉼표나 띄어쓰기에서 끊는다. 어절은 끊지 않는다</li>
 * </ul>
 * 저장은 말풍선 사이를 줄바꿈 하나로 이은 글로 하므로, 조회 때 같은 규칙으로 다시 나눠도 결과가 같다.
 */
public final class DebateBubbles {

    static final int MIN_BUBBLES = 2;
    static final int MAX_BUBBLES = 3;
    /** 이 길이 이하면 기본 2개, 넘으면 문장이 충분할 때 3개로 묶는다 */
    private static final int TWO_BUBBLE_MAX_LENGTH = 250;

    private static final Pattern SENTENCE_END = Pattern.compile("(?<=[.?!…])\\s+");

    private DebateBubbles() {
    }

    public static List<String> split(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        for (String line : text.split("\\R")) {
            String trimmed = line.strip();
            if (!trimmed.isEmpty()) {
                lines.add(trimmed);
            }
        }
        if (lines.size() >= MIN_BUBBLES && lines.size() <= MAX_BUBBLES) {
            return lines;
        }
        String joined = String.join(" ", lines);
        List<String> sentences = new ArrayList<>();
        for (String sentence : SENTENCE_END.split(joined)) {
            if (!sentence.isBlank()) {
                sentences.add(sentence.strip());
            }
        }
        if (sentences.size() < MIN_BUBBLES) {
            return splitOneSentence(joined);
        }
        int target = length(joined) <= TWO_BUBBLE_MAX_LENGTH ? MIN_BUBBLES : Math.min(MAX_BUBBLES, sentences.size());
        return group(sentences, target);
    }

    /** 저장용. 말풍선 사이를 줄바꿈 하나로 잇는다 */
    public static String join(List<String> bubbles) {
        return String.join("\n", bubbles);
    }

    /** 길이 검증용. 말풍선 사이 줄바꿈은 글자 수에 넣지 않는다 */
    public static int length(String text) {
        String flat = text.replaceAll("\\s*\\R\\s*", " ").strip();
        return flat.codePointCount(0, flat.length());
    }

    /** 문장을 순서대로 target 개 묶음으로 나눈다. 누적 길이가 전체의 i/target 에 가장 가까운 문장 경계에서 자른다 */
    private static List<String> group(List<String> sentences, int target) {
        int total = 0;
        int[] cumulative = new int[sentences.size()];
        for (int i = 0; i < sentences.size(); i++) {
            total += length(sentences.get(i));
            cumulative[i] = total;
        }
        List<Integer> cuts = new ArrayList<>();
        int previous = -1;
        for (int g = 1; g < target; g++) {
            double goal = total * g / (double) target;
            int best = -1;
            // 뒤에 남은 묶음마다 문장이 하나씩은 남도록 범위를 제한한다
            for (int i = previous + 1; i <= sentences.size() - 1 - (target - g); i++) {
                if (best < 0 || Math.abs(cumulative[i] - goal) < Math.abs(cumulative[best] - goal)) {
                    best = i;
                }
            }
            cuts.add(best);
            previous = best;
        }
        List<String> bubbles = new ArrayList<>();
        int start = 0;
        for (int cut : cuts) {
            bubbles.add(String.join(" ", sentences.subList(start, cut + 1)));
            start = cut + 1;
        }
        bubbles.add(String.join(" ", sentences.subList(start, sentences.size())));
        return bubbles;
    }

    /** 한 문장뿐일 때: 가운데에 가장 가까운 쉼표 뒤, 없으면 띄어쓰기에서 둘로 나눈다 */
    private static List<String> splitOneSentence(String sentence) {
        int middle = sentence.length() / 2;
        int cut = nearest(sentence, Pattern.compile(",\\s"), middle, 1);
        if (cut < 0) {
            cut = nearest(sentence, Pattern.compile("\\s"), middle, 0);
        }
        if (cut <= 0 || cut >= sentence.length()) {
            return List.of(sentence);
        }
        String first = sentence.substring(0, cut).strip();
        String second = sentence.substring(cut).strip();
        if (first.isEmpty() || second.isEmpty()) {
            return List.of(sentence);
        }
        return List.of(first, second);
    }

    /** pattern 이 끝나는 위치(offset 만큼 앞당긴 곳) 중 middle 에 가장 가까운 곳 */
    private static int nearest(String text, Pattern pattern, int middle, int offset) {
        Matcher matcher = pattern.matcher(text);
        int best = -1;
        while (matcher.find()) {
            int position = matcher.start() + offset;
            if (best < 0 || Math.abs(position - middle) < Math.abs(best - middle)) {
                best = position;
            }
        }
        return best;
    }
}

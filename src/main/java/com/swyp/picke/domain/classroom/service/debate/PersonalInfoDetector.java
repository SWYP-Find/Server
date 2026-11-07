package com.swyp.picke.domain.classroom.service.debate;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 반박에 섞인 연락처·식별번호를 찾는다 (PRD §11 "위협·개인정보 등 차단 대상").
 * 연도·통계처럼 숫자가 들어간 정상 논증을 막지 않도록 형식이 분명한 번호만 잡는다.
 */
final class PersonalInfoDetector {

    private static final List<Pattern> PATTERNS = List.of(
            // 휴대전화 010-1234-5678, 01012345678, 010 1234 5678
            Pattern.compile("(?<!\\d)01[016789][-.\\s]?\\d{3,4}[-.\\s]?\\d{4}(?!\\d)"),
            // 유선전화 02-123-4567, 031-1234-5678 (구분자 필수)
            Pattern.compile("(?<!\\d)0(?:2|[3-6][1-5])[-.)\\s]\\s?\\d{3,4}[-.\\s]\\d{4}(?!\\d)"),
            // 이메일
            Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"),
            // 주민등록·외국인등록번호 900101-1234567 (생년월일 형식 + 성별 자리)
            Pattern.compile("(?<!\\d)\\d{2}(?:0[1-9]|1[0-2])(?:0[1-9]|[12]\\d|3[01])\\s?-?\\s?[1-8]\\d{6}(?!\\d)"),
            // 카드번호 1234-5678-9012-3456
            Pattern.compile("(?<!\\d)\\d{4}[-\\s]\\d{4}[-\\s]\\d{4}[-\\s]\\d{4}(?!\\d)")
    );

    private PersonalInfoDetector() {
    }

    static boolean contains(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return PATTERNS.stream().anyMatch(pattern -> pattern.matcher(text).find());
    }
}

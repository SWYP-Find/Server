package com.swyp.picke.domain.ticket.util;

import java.security.SecureRandom;

/**
 * 이용권 코드 생성. 형식은 {@code PKE} + 16진수 대문자 6자리 (예: {@code PKE05D2E1}, 기획 결정 2026-11-19).
 * <p>무작위 부분이 24비트(약 1,678만 가지)라 추측 공격에 약하므로, 코드 확인·등록은
 * {@link com.swyp.picke.domain.ticket.service.TicketCodeAttemptGuard} 로 계정별 실패 횟수를 제한한다.</p>
 * <p>공백·하이픈·소문자는 {@link TicketCodeHasher#normalize} 가 흡수한다. 원문은 관리자 발급/조회 응답으로만 노출하고
 * 서버 로그에는 남기지 않는다. DB에는 별도 키로 암호화해 보관한다.</p>
 */
public final class TicketCodeGenerator {

    public static final String PREFIX = "PKE";
    public static final int RANDOM_LENGTH = 6;
    public static final int CODE_LENGTH = PREFIX.length() + RANDOM_LENGTH;
    private static final String HEX = "0123456789ABCDEF";
    private static final SecureRandom RANDOM = new SecureRandom();

    private TicketCodeGenerator() {
    }

    /** 정규화 기준과 같은 원문 (해시 입력) */
    public static String generate() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH).append(PREFIX);
        for (int i = 0; i < RANDOM_LENGTH; i++) {
            sb.append(HEX.charAt(RANDOM.nextInt(HEX.length())));
        }
        return sb.toString();
    }

    /** 사람에게 보여주는 형식. 새 형식은 원문 그대로다 */
    public static String format(String rawCode) {
        return rawCode;
    }
}

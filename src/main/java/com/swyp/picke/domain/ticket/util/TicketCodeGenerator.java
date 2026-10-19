package com.swyp.picke.domain.ticket.util;

import java.security.SecureRandom;

/**
 * 이용권 코드 생성. SecureRandom 130bit → Crockford Base32 26자 (0/O, 1/I/L 혼동 문자 제외).
 * 표시 형식은 {@code XXXXXX-XXXXX-XXXXX-XXXXX-XXXXX} (6-5-5-5-5). 하이픈·소문자는 {@link TicketCodeHasher#normalize} 가 흡수한다.
 * 원문은 발급 응답에서 1회만 노출하고 저장·로그에 남기지 않는다.
 */
public final class TicketCodeGenerator {

    public static final int CODE_LENGTH = 26;
    private static final String ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
    private static final int[] GROUPS = {6, 5, 5, 5, 5};
    private static final SecureRandom RANDOM = new SecureRandom();

    private TicketCodeGenerator() {
    }

    /** 하이픈 없는 26자 원문 (해시 입력·정규화 기준과 동일) */
    public static String generate() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    /** 사람이 받아 적기 쉬운 표시 형식 */
    public static String format(String rawCode) {
        StringBuilder sb = new StringBuilder(CODE_LENGTH + GROUPS.length - 1);
        int pos = 0;
        for (int i = 0; i < GROUPS.length; i++) {
            if (i > 0) {
                sb.append('-');
            }
            sb.append(rawCode, pos, pos + GROUPS[i]);
            pos += GROUPS[i];
        }
        return sb.toString();
    }
}

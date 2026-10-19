package com.swyp.picke.domain.classroom.util;

import com.swyp.picke.domain.classroom.entity.Classroom;
import java.security.SecureRandom;

/**
 * 클래스 초대코드 6자. 눈으로 읽고 타이핑하는 코드라 헷갈리는 글자(0/O, 1/I/L)는 뺀다.
 * 유일성은 호출 측에서 DB 조회로 보장한다.
 */
public final class InviteCodeGenerator {

    /** 숫자 2~9 + 대문자(I, L, O 제외) = 31자 */
    static final String ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";

    private static final SecureRandom RANDOM = new SecureRandom();

    private InviteCodeGenerator() {
    }

    public static String generate() {
        StringBuilder sb = new StringBuilder(Classroom.INVITE_CODE_LENGTH);
        for (int i = 0; i < Classroom.INVITE_CODE_LENGTH; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}

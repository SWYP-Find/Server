package com.swyp.picke.domain.ticket.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

/**
 * 이용권 코드 해시. DB 에는 원문 대신 이 해시(class_tickets.code_hash)만 저장한다.
 * 발급(어드민)과 등록(유저) 양쪽이 반드시 같은 정규화 + 해시를 써야 조회가 맞는다.
 * <p>정규화: 앞뒤/중간 공백과 하이픈 제거 → 대문자. 즉 "picke-ab12-cd34" 와 "PICKEAB12CD34" 는 같은 코드.</p>
 */
public final class TicketCodeHasher {

    private TicketCodeHasher() {
    }

    public static String normalize(String rawCode) {
        if (rawCode == null) {
            return "";
        }
        return rawCode.replaceAll("[\s-]", "").toUpperCase(Locale.ROOT);
    }

    /** 정규화한 코드의 SHA-256 을 소문자 hex(64자)로 반환 */
    public static String hash(String rawCode) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(normalize(rawCode).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}

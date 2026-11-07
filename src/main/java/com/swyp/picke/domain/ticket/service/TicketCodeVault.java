package com.swyp.picke.domain.ticket.service;

import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 이용권 코드를 AES-GCM으로 저장·복호화합니다. 키는 Base64 인코딩한 32바이트 값을 환경변수로 받습니다. */
@Component
public class TicketCodeVault {
    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final byte[] keyBytes;

    public TicketCodeVault(@Value("${picke.ticket.code-encryption-key:}") String encodedKey) {
        byte[] decoded = new byte[0];
        if (encodedKey != null && !encodedKey.isBlank()) {
            try {
                decoded = Base64.getDecoder().decode(encodedKey.trim());
            } catch (IllegalArgumentException ignored) {
                // Report one consistent configuration error when the vault is used.
            }
        }
        this.keyBytes = decoded.length == 32 ? decoded : new byte[0];
    }

    public String encrypt(String code) {
        byte[] nonce = new byte[NONCE_BYTES];
        RANDOM.nextBytes(nonce);
        byte[] encrypted = crypt(Cipher.ENCRYPT_MODE, code, nonce);
        return Base64.getEncoder().encodeToString(ByteBuffer.allocate(nonce.length + encrypted.length)
                .put(nonce).put(encrypted).array());
    }

    public String decrypt(String encryptedCode) {
        byte[] payload;
        try {
            payload = Base64.getDecoder().decode(encryptedCode);
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.TICKET_CODE_STORAGE_UNAVAILABLE);
        }
        if (payload.length <= NONCE_BYTES) {
            throw new CustomException(ErrorCode.TICKET_CODE_STORAGE_UNAVAILABLE);
        }
        byte[] nonce = new byte[NONCE_BYTES];
        byte[] ciphertext = new byte[payload.length - NONCE_BYTES];
        System.arraycopy(payload, 0, nonce, 0, NONCE_BYTES);
        System.arraycopy(payload, NONCE_BYTES, ciphertext, 0, ciphertext.length);
        return new String(crypt(Cipher.DECRYPT_MODE, ciphertext, nonce), java.nio.charset.StandardCharsets.UTF_8);
    }

    private byte[] crypt(int mode, String value, byte[] nonce) {
        return crypt(mode, value.getBytes(java.nio.charset.StandardCharsets.UTF_8), nonce);
    }

    private byte[] crypt(int mode, byte[] value, byte[] nonce) {
        if (keyBytes.length != 32) {
            throw new CustomException(ErrorCode.TICKET_CODE_STORAGE_UNAVAILABLE);
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(mode, new SecretKeySpec(keyBytes, "AES"), new GCMParameterSpec(TAG_BITS, nonce));
            return cipher.doFinal(value);
        } catch (GeneralSecurityException e) {
            throw new CustomException(ErrorCode.TICKET_CODE_STORAGE_UNAVAILABLE);
        }
    }
}

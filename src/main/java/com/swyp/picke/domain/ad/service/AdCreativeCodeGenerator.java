package com.swyp.picke.domain.ad.service;

import com.swyp.picke.domain.ad.repository.AdCreativeRepository;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import java.security.SecureRandom;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 공개 클릭 URL(/c/{code})에 노출되는 짧은 코드를 만든다.
 * 어드민이 눈으로 옮겨 적는 일이 있어 헷갈리기 쉬운 글자(l, o, 0, 1)를 뺀다.
 */
@Component
@RequiredArgsConstructor
public class AdCreativeCodeGenerator {

    private static final String ALPHABET = "abcdefghijkmnpqrstuvwxyz23456789";
    private static final int LENGTH = 8;
    private static final int MAX_ATTEMPTS = 10;

    private final AdCreativeRepository adCreativeRepository;
    private final SecureRandom random = new SecureRandom();

    public String generate() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String code = randomCode();
            if (!adCreativeRepository.existsByCode(code)) {
                return code;
            }
        }
        throw new CustomException(ErrorCode.AD_CODE_GENERATION_FAILED);
    }

    private String randomCode() {
        return random.ints(LENGTH, 0, ALPHABET.length())
                .mapToObj(index -> String.valueOf(ALPHABET.charAt(index)))
                .collect(Collectors.joining());
    }
}

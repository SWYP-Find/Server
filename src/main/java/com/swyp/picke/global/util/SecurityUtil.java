package com.swyp.picke.global.util;

import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtil {
    public static Long getCurrentUserId() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
        }

        // JwtFilter에서 userId(Long)를 principal로 넣었으므로 캐스팅해서 반환
        return (Long) authentication.getPrincipal();
    }
}

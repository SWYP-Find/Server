package com.swyp.app.domain.oauth.jwt;

import com.swyp.app.global.common.exception.CustomException;
import com.swyp.app.global.common.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    // 인증 제외 경로
    private static final List<String> WHITELIST = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/swagger-ui",
            "/v3/api-docs"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestUri = request.getRequestURI();

        // 화이트리스트 경로는 토큰 검증 스킵
        if (isWhitelisted(requestUri)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Authorization 헤더에서 토큰 추출
        String token = resolveToken(request);

        if (token == null) {
            throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
        }

        if (!jwtProvider.validateToken(token)) {
            throw new CustomException(ErrorCode.AUTH_ACCESS_TOKEN_EXPIRED);
        }

        // 토큰에서 userId와 role 추출
        Long userId = jwtProvider.getUserId(token);
        String role = jwtProvider.getRole(token);

        // 권한 문자열에 "ROLE_" 접두사가 없으면 붙여줌 (스프링 시큐리티 규칙)
        String authorityName = (role != null && role.startsWith("ROLE_")) ? role : "ROLE_" + role;

        // 추출한 권한을 SecurityContext 에 주입
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        role != null ? List.of(new SimpleGrantedAuthority(authorityName)) : List.of()
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private boolean isWhitelisted(String uri) {
        return WHITELIST.stream().anyMatch(uri::startsWith);
    }
}
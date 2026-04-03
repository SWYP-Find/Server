package com.swyp.picke.domain.oauth.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swyp.picke.global.common.exception.ErrorCode;
import com.swyp.picke.global.common.response.ApiResponse;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 1. 스웨거 및 인증 관련 경로를 더 넓게 잡았습니다.
    private static final List<String> WHITELIST = List.of(
            "/api/v1/admob/reward",
            "/swagger-ui",
            "/v3/api-docs",
            "/api/v1/admin/login",
            "/api/v1/admin/picke",
            "/js",
            "/css",
            "/images",
            "/favicon.ico",
            "/api/v1/auth",      // 로그인, 리프레시 등 인증 관련 전체
            "/swagger-ui",       // 스웨거 UI 리소스 전체
            "/v3/api-docs",      // OpenAPI 스펙 전체
            "/api/v1/home",      // 홈 화면
            "/api/v1/notices",   // 공지사항
            "/api/test",         // 테스트용
            "/result",           // 공유 링크 리다이렉트
            "/api/v1/resources" // 이미지, 오디오 파일 (Presigned URL)
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestUri = request.getRequestURI();
        boolean isWhitelist = isWhitelisted(requestUri);

        log.info("[JwtFilter Debug] URI: {}, isWhitelisted: {}", requestUri, isWhitelist);

        try {
            // 1. 화이트리스트 검사 전, 무조건 토큰부터 꺼냅니다.
            String token = resolveToken(request);

            if (token != null) {
                // 2. 토큰이 존재하면 유효성을 검사합니다.
                if (!jwtProvider.validateToken(token)) {
                    log.error("[JwtFilter] Invalid or Expired token for URI: {}", requestUri);
                    setErrorResponse(response, ErrorCode.AUTH_ACCESS_TOKEN_EXPIRED);
                    return;
                }

                // 3. 토큰이 유효하다면 SecurityContext에 유저 정보(userId)를 저장합니다.
                Long userId = jwtProvider.getUserId(token);
                String role = jwtProvider.getRole(token);
                String authorityName = (role != null && role.startsWith("ROLE_")) ? role : "ROLE_" + role;

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                role != null ? List.of(new SimpleGrantedAuthority(authorityName)) : List.of()
                        );

                SecurityContextHolder.getContext().setAuthentication(authentication);

            } else {
                // 4. 토큰이 비어있을 때, 화이트리스트(홈 화면 등)가 아니라면 에러를 던집니다.
                if (!isWhitelist) {
                    log.warn("[JwtFilter] Token missing for URI: {}", requestUri);
                    setErrorResponse(response, ErrorCode.AUTH_UNAUTHORIZED);
                    return;
                }
            }

            // 5. [토큰 검증을 무사히 마쳤거나] or [토큰이 없는 비회원인데 화이트리스트인 경우] 다음 필터로 넘어갑니다.
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("[JwtFilter] Filter Error: {}", e.getMessage());
            setErrorResponse(response, ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private void setErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(errorCode.getHttpStatus().value());

        ApiResponse<Void> errorResponse = ApiResponse.onFailure(
                errorCode.getHttpStatus().value(),
                errorCode.getCode(),
                errorCode.getMessage()
        );

        String result = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(result);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private boolean isWhitelisted(String uri) {
        // 1. URI가 화이트리스트의 어떤 값으로든 시작하면 true
        return WHITELIST.stream().anyMatch(uri::startsWith);
    }
}
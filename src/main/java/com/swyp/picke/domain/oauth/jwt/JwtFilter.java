package com.swyp.picke.domain.oauth.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swyp.picke.domain.user.repository.UserDailyActivityRepository;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserDailyActivityRepository userDailyActivityRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 1. 화이트리스트 상수 관리
    private static final List<String> WHITELIST = List.of(
            "/api/v1/admob/reward",
            "/api/v1/admin/login",
            "/api/v1/admin/picke",
            "/js",
            "/css",
            "/images",
            "/favicon.ico",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/swagger-ui",       
            "/v3/api-docs",      
            "/api/v1/home",      
            "/api/v1/notices",   
            "/api/test",         
            "/result",           
            "/report",           
            "/battle",           
            "/api/v1/share/recap/", 
            "/.well-known",     
            "/api/v1/resources",
            "/app-ads.txt",
            "/terms",
            "/privacy-policy",
            "/robots.txt",
            "/c/",
            "/api/v1/ads"
    );

    /**
     * WHITELIST는 startsWith로 매칭하므로 "/"를 넣으면 전체 인증이 무력화된다.
     * 루트와 에러 포워딩처럼 정확히 일치할 때만 열어야 하는 경로는 여기 둔다.
     *
     * <p>/error가 빠져 있으면 존재하지 않는 경로가 404 대신 401로 나온다.
     * 스프링이 404를 /error로 포워딩하는데 그 경로가 다시 인증에 막히기 때문이다.
     */
    private static final Set<String> EXACT_WHITELIST = Set.of(
            "/",
            "/error"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String requestUri = request.getRequestURI();
        return isWhitelisted(requestUri);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestUri = request.getRequestURI();
        log.info("[JwtFilter Debug] Processing URI: {}", requestUri);

        try {
            String token = resolveToken(request);

            if (token != null) {
                if (!jwtProvider.validateToken(token)) {
                    log.error("[JwtFilter] Invalid or Expired token for URI: {}", requestUri);
                    setErrorResponse(response, ErrorCode.AUTH_ACCESS_TOKEN_EXPIRED);
                    return;
                }

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
                recordActivity(userId);

            } else {
                // shouldNotFilter가 잡지 못한 주소 중 토큰이 없다면 무조건 401 차단
                log.warn("[JwtFilter] Token missing for secured URI: {}", requestUri);
                setErrorResponse(response, ErrorCode.AUTH_UNAUTHORIZED);
                return;
            }

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("[JwtFilter] Filter Error: {}", e.getMessage());
            setErrorResponse(response, ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * DAU/MAU 집계를 위한 활동 기록. 대시보드 집계용 부가 기능이라 실패해도 인증 흐름을 막지 않는다.
     */
    private void recordActivity(Long userId) {
        try {
            userDailyActivityRepository.markActive(userId, LocalDate.now());
        } catch (Exception e) {
            log.warn("[JwtFilter] Failed to record daily activity for userId={}: {}", userId, e.getMessage());
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
        if (EXACT_WHITELIST.contains(uri)) {
            return true;
        }
        return WHITELIST.stream().anyMatch(white -> uri.equals(white) || uri.startsWith(white));
    }
}
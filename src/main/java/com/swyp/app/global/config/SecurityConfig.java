package com.swyp.app.global.config;

import com.swyp.app.domain.oauth.jwt.JwtFilter;
import com.swyp.app.domain.oauth.jwt.JwtProvider;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class
SecurityConfig {

    private final JwtProvider jwtProvider;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/api/v1/auth/**", "/api/v1/home",
                                "/api/v1/notices/**",
                                "/api/test/**",
                                "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**",
                                "/js/**", "/css/**", "/images/**", "/favicon.ico",
                                "/api/v1/admin/login", "/api/v1/admin",
                                "/result/**"
                        ).permitAll()

                        // 2. 관리자 HTML 화면 렌더링 요청
                        .requestMatchers(HttpMethod.GET, "/api/v1/admin/picke/**").permitAll()

                        // 3. 단순 조회성 REST API (JS가 헤더에 토큰 실어서 요청)
                        .requestMatchers(HttpMethod.GET, "/api/v1/tags", "/api/v1/battles/**").authenticated()

                        // 4. 관리자 전용 데이터 조작 REST API (생성, 수정, 삭제)
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new JwtFilter(jwtProvider),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 허용할 오리진(도메인) 설정
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "https://picke.store",
                "https://www.picke.store"
        ));

        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // 허용할 헤더
        configuration.setAllowedHeaders(List.of("*"));

        // 자격 증명(쿠키, Authorization 헤더 등) 허용
        configuration.setAllowCredentials(true);

        // 모든 경로(/**)에 대해 위 설정 적용
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

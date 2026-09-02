package com.swyp.picke.domain.oauth.jwt;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.swyp.picke.domain.user.repository.UserDailyActivityRepository;
import jakarta.servlet.FilterChain;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private UserDailyActivityRepository userDailyActivityRepository;

    @Mock
    private FilterChain filterChain;

    @Test
    void doFilterInternal_유효한_토큰이면_오늘자_활동을_기록한다() throws Exception {
        JwtFilter jwtFilter = new JwtFilter(jwtProvider, userDailyActivityRepository);

        when(jwtProvider.validateToken("valid-token")).thenReturn(true);
        when(jwtProvider.getUserId("valid-token")).thenReturn(42L);
        when(jwtProvider.getRole("valid-token")).thenReturn("USER");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/me/mypage");
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(userDailyActivityRepository).markActive(eq(42L), any(LocalDate.class));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_활동_기록이_실패해도_요청_처리는_계속된다() throws Exception {
        JwtFilter jwtFilter = new JwtFilter(jwtProvider, userDailyActivityRepository);

        when(jwtProvider.validateToken("valid-token")).thenReturn(true);
        when(jwtProvider.getUserId("valid-token")).thenReturn(42L);
        when(jwtProvider.getRole("valid-token")).thenReturn("USER");
        doThrow(new RuntimeException("DB down"))
                .when(userDailyActivityRepository).markActive(any(), any());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/me/mypage");
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}

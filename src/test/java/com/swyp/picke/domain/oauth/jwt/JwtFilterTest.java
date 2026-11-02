package com.swyp.picke.domain.oauth.jwt;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.enums.UserStatus;
import com.swyp.picke.domain.user.repository.UserDailyActivityRepository;
import com.swyp.picke.domain.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
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
    private UserRepository userRepository;

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

    @ParameterizedTest
    @EnumSource(value = UserStatus.class, names = {"ACTIVE", "PENDING"})
    void doFilterInternal_정상_계정은_요청을_통과시킨다(UserStatus status) throws Exception {
        JwtFilter jwtFilter = new JwtFilter(jwtProvider, userDailyActivityRepository, userRepository);
        stubValidToken();
        User user = mock(User.class);
        when(user.getStatus()).thenReturn(status);
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));

        MockHttpServletRequest request = authorizedRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @ParameterizedTest
    @CsvSource({"SUSPENDED,403", "BANNED,403", "DELETED,401"})
    void doFilterInternal_막힌_계정은_요청을_거절한다(UserStatus status, int httpStatus) throws Exception {
        JwtFilter jwtFilter = new JwtFilter(jwtProvider, userDailyActivityRepository, userRepository);
        when(jwtProvider.validateToken("valid-token")).thenReturn(true);
        when(jwtProvider.getUserId("valid-token")).thenReturn(42L);
        User user = mock(User.class);
        when(user.getStatus()).thenReturn(status);
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));

        MockHttpServletRequest request = authorizedRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(httpStatus);
    }

    @Test
    void doFilterInternal_없는_계정은_401로_거절한다() throws Exception {
        JwtFilter jwtFilter = new JwtFilter(jwtProvider, userDailyActivityRepository, userRepository);
        when(jwtProvider.validateToken("valid-token")).thenReturn(true);
        when(jwtProvider.getUserId("valid-token")).thenReturn(42L);
        when(userRepository.findById(42L)).thenReturn(Optional.empty());

        MockHttpServletRequest request = authorizedRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(401);
    }

    private void stubValidToken() {
        when(jwtProvider.validateToken("valid-token")).thenReturn(true);
        when(jwtProvider.getUserId("valid-token")).thenReturn(42L);
        when(jwtProvider.getRole("valid-token")).thenReturn("USER");
    }

    private static MockHttpServletRequest authorizedRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/me/mypage");
        request.addHeader("Authorization", "Bearer valid-token");
        return request;
    }
}

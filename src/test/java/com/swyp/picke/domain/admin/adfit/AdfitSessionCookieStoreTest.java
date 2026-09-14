package com.swyp.picke.domain.admin.adfit;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdfitSessionCookieStoreTest {

    @Mock AdfitSessionCookieRepository repository;
    @InjectMocks AdfitSessionCookieStore store;

    private void environment(String cookie) {
        ReflectionTestUtils.setField(store, "environmentCookie", cookie);
    }

    @Test
    @DisplayName("관리자가 넣은 쿠키가 환경변수보다 앞선다. 만료 때 재배포 없이 갈아끼우는 게 목적이다")
    void adminCookieWinsOverEnvironment() {
        environment("ENV=old");
        when(repository.findTopByOrderByIdDesc())
                .thenReturn(Optional.of(new AdfitSessionCookie("ADMIN=new")));

        assertThat(store.current()).isEqualTo("ADMIN=new");
        assertThat(store.status().source()).isEqualTo(AdfitSessionCookieSource.ADMIN_CONSOLE);
    }

    @Test
    @DisplayName("저장된 쿠키가 없으면 환경변수 값을 쓴다")
    void fallsBackToEnvironment() {
        environment("ENV=value");
        when(repository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());

        assertThat(store.current()).isEqualTo("ENV=value");
        assertThat(store.status().source()).isEqualTo(AdfitSessionCookieSource.ENVIRONMENT);
    }

    @Test
    @DisplayName("양쪽 모두 없으면 미설정이다. 자동 조회가 NOT_CONFIGURED 로 떨어진다")
    void reportsNotConfigured() {
        environment("");
        when(repository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());

        assertThat(store.current()).isEmpty();
        assertThat(store.status().configured()).isFalse();
        assertThat(store.status().source()).isEqualTo(AdfitSessionCookieSource.NONE);
    }

    @Test
    @DisplayName("상태에는 쿠키 값을 담지 않는다. 콘솔 로그인 자격이라 읽히면 계정이 열린다")
    void statusNeverCarriesCookieValue() {
        environment("");
        when(repository.findTopByOrderByIdDesc())
                .thenReturn(Optional.of(new AdfitSessionCookie("KAKAO=secret")));

        assertThat(store.status().toString()).doesNotContain("secret");
    }

    @Test
    @DisplayName("name=value 형태가 아니면 저장하지 않는다. 조회가 조용히 실패하게 두지 않는다")
    void rejectsNonCookieInput() {
        assertThatThrownBy(() -> store.update("붙여넣기_실수"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("환경변수가 비어 있어도 빈이 만들어진다. ADFIT_SESSION_COOKIE 미설정이 기동을 막지 않아야 한다")
    void springCreatesStoreWithoutConfiguredCookie() {
        try (var context = new org.springframework.context.annotation.AnnotationConfigApplicationContext()) {
            context.registerBean(AdfitSessionCookieRepository.class, () -> repository);
            context.register(AdfitSessionCookieStore.class);
            context.refresh();
            when(repository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());

            assertThat(context.getBean(AdfitSessionCookieStore.class).current()).isEmpty();
        }
    }

    @Test
    @DisplayName("이미 저장된 행이 있으면 새로 만들지 않고 값만 바꾼다")
    void updatesExistingRow() {
        AdfitSessionCookie stored = new AdfitSessionCookie("OLD=1");
        when(repository.findTopByOrderByIdDesc()).thenReturn(Optional.of(stored));

        store.update("  NEW=2  ");

        assertThat(stored.getValue()).isEqualTo("NEW=2");
        verify(repository).save(stored);
    }
}

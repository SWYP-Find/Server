package com.swyp.picke.domain.admin.adfit;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 자동 수익 조회에 쓸 AdFit 세션 쿠키를 고른다.
 *
 * <p>관리자가 화면에서 넣은 값이 환경변수보다 앞선다. 만료될 때 재배포 없이 갈아끼우는 게 목적이다.
 * 값은 어떤 응답에도 담지 않는다. 콘솔 로그인 자격이라 읽히면 계정이 그대로 열린다.
 */
@Component
@RequiredArgsConstructor
public class AdfitSessionCookieStore {

    private final AdfitSessionCookieRepository repository;

    @Value("${picke.adfit.session-cookie:${ADFIT_SESSION_COOKIE:}}")
    private String environmentCookie;

    @Transactional(readOnly = true)
    public String current() {
        return stored()
                .map(AdfitSessionCookie::getValue)
                .orElse(environmentCookie);
    }

    @Transactional
    public void update(String cookie) {
        String trimmed = cookie == null ? "" : cookie.trim();
        if (!trimmed.contains("=")) {
            // 브라우저에서 Cookie 헤더가 아닌 걸 붙여넣은 경우다. 조회가 조용히 실패하게 두지 않는다.
            throw new IllegalArgumentException("세션 쿠키는 name=value 형태여야 합니다.");
        }
        AdfitSessionCookie target = repository.findTopByOrderByIdDesc()
                .orElseGet(() -> new AdfitSessionCookie(trimmed));
        target.update(trimmed);
        repository.save(target);
    }

    /** 화면에 쿠키가 있는지와 마지막 갱신 시각만 준다. 값은 주지 않는다. */
    @Transactional(readOnly = true)
    public AdfitSessionCookieStatus status() {
        return stored()
                .map(cookie -> new AdfitSessionCookieStatus(
                        true, AdfitSessionCookieSource.ADMIN_CONSOLE, cookie.getUpdatedAt()))
                .orElseGet(() -> StringUtils.hasText(environmentCookie)
                        ? new AdfitSessionCookieStatus(true, AdfitSessionCookieSource.ENVIRONMENT, null)
                        : new AdfitSessionCookieStatus(false, AdfitSessionCookieSource.NONE, null));
    }

    private Optional<AdfitSessionCookie> stored() {
        return repository.findTopByOrderByIdDesc()
                .filter(cookie -> StringUtils.hasText(cookie.getValue()));
    }
}

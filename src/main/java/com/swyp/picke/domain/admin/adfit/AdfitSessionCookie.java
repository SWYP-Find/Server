package com.swyp.picke.domain.admin.adfit;

import com.swyp.picke.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * AdFit 콘솔 세션 쿠키.
 *
 * <p>AdFit은 매체주 수익을 읽는 공개 REST API가 없어 콘솔 내부 API를 세션 쿠키로 호출한다.
 * 이 쿠키는 수시로 만료되는데 값이 환경변수에만 있으면 만료마다 재배포를 해야 한다.
 * 관리자가 화면에서 갈아끼울 수 있도록 한 행으로 보관한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "adfit_session_cookies")
public class AdfitSessionCookie extends BaseEntity {

    @Column(name = "cookie_value", nullable = false, length = 4096)
    private String value;

    public AdfitSessionCookie(String value) {
        this.value = value;
    }

    public void update(String value) {
        this.value = value;
    }
}

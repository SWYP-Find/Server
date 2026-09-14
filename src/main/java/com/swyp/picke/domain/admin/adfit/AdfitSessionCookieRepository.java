package com.swyp.picke.domain.admin.adfit;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdfitSessionCookieRepository extends JpaRepository<AdfitSessionCookie, Long> {
    /** 한 행만 두고 쓰지만, 저장이 겹쳐 행이 늘어도 마지막 값이 이기게 한다. */
    Optional<AdfitSessionCookie> findTopByOrderByIdDesc();
}

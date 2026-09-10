package com.swyp.picke.domain.admin.adfit;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdfitDailyRepository extends JpaRepository<AdfitDaily, Long> {
    Optional<AdfitDaily> findByDateAndUnit(LocalDate date, AdfitUnit unit);
    List<AdfitDaily> findAllByDateBetweenOrderByDateDesc(LocalDate from, LocalDate to);
}

package com.swyp.picke.domain.admin.adfit;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AdfitReport(LocalDate from, LocalDate to, String source,
                          List<UnitReport> units, List<Day> days) {
    public record UnitReport(AdfitUnit unit, String name, List<String> placements, String format,
                             long reportedDays, long expectedDays, BigDecimal revenue,
                             BigDecimal cost, BigDecimal roi) {}
    public record Day(LocalDate date, AdfitUnit unit, BigDecimal revenue, BigDecimal cost,
                      AdfitCostBasis costBasis) {}
}

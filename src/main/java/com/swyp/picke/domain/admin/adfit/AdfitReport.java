package com.swyp.picke.domain.admin.adfit;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record AdfitReport(LocalDate from, LocalDate to, String source,
                          List<UnitReport> units, List<Day> days, AccountReport account) {
    public record UnitReport(AdfitUnit unit, String name, List<String> placements, String format,
                             long reportedDays, long expectedDays, BigDecimal revenue,
                             BigDecimal cost, BigDecimal roi) {}
    public record Day(LocalDate date, AdfitUnit unit, BigDecimal revenue, BigDecimal cost,
                      AdfitCostBasis costBasis) {}
    public record AccountReport(AdfitAccountReportStatus status, Instant fetchedAt,
                                long reportedDays, long expectedDays, BigDecimal revenue,
                                BigDecimal cost, BigDecimal roi, List<AccountDay> days) {}
    public record AccountDay(LocalDate date, BigDecimal revenue, BigDecimal ctr, BigDecimal ecpm,
                             BigDecimal fillRate, BigDecimal winFillRate) {}
}

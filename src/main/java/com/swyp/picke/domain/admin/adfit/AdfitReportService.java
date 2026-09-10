package com.swyp.picke.domain.admin.adfit;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdfitReportService {
    private final AdfitDailyRepository repository;

    @Transactional
    public void save(AdfitDailyRequest request) {
        if (request.date().isAfter(LocalDate.now(ZoneId.of("Asia/Seoul")))) {
            throw new IllegalArgumentException("미래 날짜의 수익은 입력할 수 없습니다.");
        }
        AdfitDaily daily = repository.findByDateAndUnit(request.date(), request.unit())
                .orElseGet(() -> new AdfitDaily(request.date(), request.unit()));
        daily.update(request.revenue(), request.cost(), request.costBasis());
        repository.save(daily);
    }

    @Transactional(readOnly = true)
    public AdfitReport report(LocalDate from, LocalDate to) {
        long expected = ChronoUnit.DAYS.between(from, to) + 1;
        if (expected < 1 || expected > 366) {
            throw new IllegalArgumentException("조회 기간은 1일부터 366일까지입니다.");
        }
        List<AdfitDaily> days = repository.findAllByDateBetweenOrderByDateDesc(from, to);
        List<AdfitReport.UnitReport> units = Arrays.stream(AdfitUnit.values()).map(unit -> {
            List<AdfitDaily> entries = days.stream().filter(day -> day.getUnit() == unit).toList();
            BigDecimal revenue = entries.isEmpty() ? null : entries.stream().map(AdfitDaily::getRevenue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal cost = entries.isEmpty() ? null : entries.stream().map(AdfitDaily::getCost)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            // 기간이 빠졌거나 비용 기준이 섞이면 비교 가능한 ROI가 아니다.
            boolean complete = entries.size() == expected
                    && entries.stream().map(AdfitDaily::getCostBasis).distinct().count() == 1;
            BigDecimal roi = complete && cost != null && cost.signum() > 0
                    ? revenue.subtract(cost).multiply(BigDecimal.valueOf(100))
                        .divide(cost, 2, RoundingMode.HALF_UP) : null;
            return new AdfitReport.UnitReport(unit, unit.getDisplayName(), unit.getPlacements(),
                    unit.getFormat(), entries.size(), expected, revenue, cost, roi);
        }).toList();
        return new AdfitReport(from, to, "MANUAL_CONSOLE", units, days.stream().map(day ->
                new AdfitReport.Day(day.getDate(), day.getUnit(), day.getRevenue(), day.getCost(),
                        day.getCostBasis())).toList());
    }
}
